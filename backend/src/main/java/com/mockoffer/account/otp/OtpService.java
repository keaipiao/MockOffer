package com.mockoffer.account.otp;

import com.mockoffer.account.email.EmailSender;
import com.mockoffer.common.BizException;
import com.mockoffer.common.Emails;
import java.security.SecureRandom;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** 邮箱验证码：Redis 存码（TTL、一次性）+ 限流（发送间隔、每日上限、校验失败锁定）。 */
@Service
public class OtpService {

    private static final int MAX_FAIL = 5;

    private final StringRedisTemplate redis;
    private final EmailSender sender;
    private final long ttlSeconds;
    private final long intervalSeconds;
    private final long dailyLimit;
    private final SecureRandom random = new SecureRandom();

    public OtpService(StringRedisTemplate redis, EmailSender sender,
            @Value("${app.otp.ttl-seconds}") long ttlSeconds,
            @Value("${app.otp.resend-interval-seconds}") long intervalSeconds,
            @Value("${app.otp.daily-limit}") long dailyLimit) {
        this.redis = redis;
        this.sender = sender;
        this.ttlSeconds = ttlSeconds;
        this.intervalSeconds = intervalSeconds;
        this.dailyLimit = dailyLimit;
    }

    /** 发码：先占限流名额 → 写码 → 发送；发送失败则删码 + 回退计数。 */
    public void sendCode(String rawEmail) {
        String email = Emails.normalize(rawEmail);
        if (!Emails.isValid(email)) {
            throw new BizException(40001, "邮箱格式不正确");
        }
        String intervalKey = "otp:intv:" + email;
        String dailyKey = "otp:daily:" + email;
        String codeKey = "otp:code:" + email;

        if (Boolean.TRUE.equals(redis.hasKey(intervalKey))) {
            throw new BizException(42901, "发送过于频繁，请稍后再试");
        }
        Long daily = redis.opsForValue().increment(dailyKey);
        if (daily != null && daily == 1L) {
            redis.expire(dailyKey, Duration.ofDays(1));
        }
        if (daily != null && daily > dailyLimit) {
            throw new BizException(42902, "今日验证码发送次数已达上限");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        redis.opsForValue().set(codeKey, code, Duration.ofSeconds(ttlSeconds));
        redis.opsForValue().set(intervalKey, "1", Duration.ofSeconds(intervalSeconds));
        try {
            sender.sendOtp(email, code);
        } catch (RuntimeException e) {
            redis.delete(codeKey);
            redis.delete(intervalKey);
            redis.opsForValue().decrement(dailyKey);
            throw new BizException(50001, "验证码发送失败，请稍后再试");
        }
    }

    /** 校验：失败累计到上限锁定；成功即删码。 */
    public void verify(String rawEmail, String code) {
        String email = Emails.normalize(rawEmail);
        String failKey = "otp:fail:" + email;
        String codeKey = "otp:code:" + email;

        String failStr = redis.opsForValue().get(failKey);
        long fail = failStr == null ? 0L : Long.parseLong(failStr);
        if (fail >= MAX_FAIL) {
            throw new BizException(42903, "错误次数过多，请重新获取验证码");
        }
        String stored = redis.opsForValue().get(codeKey);
        if (stored == null) {
            throw new BizException(40002, "验证码已过期，请重新获取");
        }
        if (!stored.equals(code)) {
            Long f = redis.opsForValue().increment(failKey);
            if (f != null && f == 1L) {
                redis.expire(failKey, Duration.ofSeconds(ttlSeconds));
            }
            throw new BizException(40003, "验证码错误");
        }
        redis.delete(codeKey);
        redis.delete(failKey);
    }
}
