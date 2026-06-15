package com.mockoffer.account.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mockoffer.account.email.EmailSender;
import com.mockoffer.common.BizException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** OtpService 单测：发码限流（间隔/每日上限）、校验（正确/错误/过期/锁定/一次性消费）。Redis 全 mock。 */
@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String EMAIL = "a@b.com";
    private static final String CODE_KEY = "otp:code:a@b.com";
    private static final String INTV_KEY = "otp:intv:a@b.com";
    private static final String DAILY_KEY = "otp:daily:a@b.com";
    private static final String FAIL_KEY = "otp:fail:a@b.com";

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;
    @Mock EmailSender sender;

    OtpService otp;

    @BeforeEach
    void setup() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        otp = new OtpService(redis, sender, 600, 60, 10);
    }

    // ---------- sendCode ----------

    @Test
    void 邮箱格式错误_拒绝40001() {
        BizException e = catchThrowableOfType(BizException.class, () -> otp.sendCode("not-an-email"));
        assertThat(e.getCode()).isEqualTo(40001);
    }

    @Test
    void 发码间隔内重复_拒绝42901() {
        when(redis.hasKey(INTV_KEY)).thenReturn(true);

        BizException e = catchThrowableOfType(BizException.class, () -> otp.sendCode(EMAIL));
        assertThat(e.getCode()).isEqualTo(42901);
    }

    @Test
    void 当日发码超上限_拒绝42902() {
        when(valueOps.increment(DAILY_KEY)).thenReturn(11L);

        BizException e = catchThrowableOfType(BizException.class, () -> otp.sendCode(EMAIL));
        assertThat(e.getCode()).isEqualTo(42902);
    }

    @Test
    void 发码成功_写码写间隔锁并发送() {
        when(valueOps.increment(DAILY_KEY)).thenReturn(1L);

        otp.sendCode(EMAIL);

        verify(redis).expire(eq(DAILY_KEY), any(Duration.class));
        verify(valueOps).set(eq(CODE_KEY), anyString(), any(Duration.class));
        verify(valueOps).set(eq(INTV_KEY), eq("1"), any(Duration.class));
        verify(sender).sendOtp(eq(EMAIL), anyString());
    }

    @Test
    void 发送失败_回退计数与删码并抛50001() {
        when(valueOps.increment(DAILY_KEY)).thenReturn(1L);
        doThrow(new RuntimeException("smtp down")).when(sender).sendOtp(anyString(), anyString());

        BizException e = catchThrowableOfType(BizException.class, () -> otp.sendCode(EMAIL));

        assertThat(e.getCode()).isEqualTo(50001);
        verify(redis).delete(CODE_KEY);
        verify(redis).delete(INTV_KEY);
        verify(valueOps).decrement(DAILY_KEY);
    }

    // ---------- verify ----------

    @Test
    void 校验码正确_消费并清除() {
        when(valueOps.get(FAIL_KEY)).thenReturn(null);
        when(valueOps.get(CODE_KEY)).thenReturn("123456");

        otp.verify(EMAIL, "123456");

        verify(redis).delete(CODE_KEY);
        verify(redis).delete(FAIL_KEY);
    }

    @Test
    void 校验码错误_累计失败抛40003() {
        when(valueOps.get(FAIL_KEY)).thenReturn(null);
        when(valueOps.get(CODE_KEY)).thenReturn("123456");
        when(valueOps.increment(FAIL_KEY)).thenReturn(1L);

        BizException e = catchThrowableOfType(BizException.class, () -> otp.verify(EMAIL, "000000"));
        assertThat(e.getCode()).isEqualTo(40003);
    }

    @Test
    void 验证码已过期_抛40002() {
        when(valueOps.get(FAIL_KEY)).thenReturn(null);
        when(valueOps.get(CODE_KEY)).thenReturn(null);

        BizException e = catchThrowableOfType(BizException.class, () -> otp.verify(EMAIL, "123456"));
        assertThat(e.getCode()).isEqualTo(40002);
    }

    @Test
    void 失败次数达上限_锁定42903() {
        when(valueOps.get(FAIL_KEY)).thenReturn("5");

        BizException e = catchThrowableOfType(BizException.class, () -> otp.verify(EMAIL, "123456"));
        assertThat(e.getCode()).isEqualTo(42903);
    }
}
