package com.mockoffer.account.security;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 会话相关的 Redis 存储：refresh token（可吊销 + 轮换）与 access 黑名单（即时吊销）。 */
@Component
public class RefreshTokenStore {

    private static final String RT = "rt:";   // refresh token -> userId
    private static final String BL = "bl:";   // 被拉黑的 access jti

    private final StringRedisTemplate redis;

    public RefreshTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void saveRefresh(String token, long userId, long ttlSeconds) {
        redis.opsForValue().set(RT + token, Long.toString(userId), Duration.ofSeconds(ttlSeconds));
    }

    /** 原子取出并删除（GETDEL）：保证一个 refresh 只能被消费一次，配合轮换。 */
    public Long consumeRefresh(String token) {
        String v = redis.opsForValue().getAndDelete(RT + token);
        return v == null ? null : Long.valueOf(v);
    }

    public void deleteRefresh(String token) {
        redis.delete(RT + token);
    }

    public void blacklistAccess(String jti, long ttlSeconds) {
        if (jti != null && ttlSeconds > 0) {
            redis.opsForValue().set(BL + jti, "1", Duration.ofSeconds(ttlSeconds));
        }
    }

    public boolean isAccessBlacklisted(String jti) {
        return jti != null && Boolean.TRUE.equals(redis.hasKey(BL + jti));
    }
}
