package com.mockoffer.account.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** RefreshTokenStore 单测：refresh 一次性消费（轮换/重放拒绝）、access 黑名单（即时吊销）。 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenStoreTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;
    @InjectMocks RefreshTokenStore store;

    @BeforeEach
    void setup() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void 保存refresh写入rt键() {
        store.saveRefresh("tok", 9L, 100);

        verify(valueOps).set(eq("rt:tok"), eq("9"), any(Duration.class));
    }

    @Test
    void 消费refresh第一次拿到用户重放返回null() {
        when(valueOps.getAndDelete("rt:tok")).thenReturn("7", null);

        assertThat(store.consumeRefresh("tok")).isEqualTo(7L);
        assertThat(store.consumeRefresh("tok")).isNull();
    }

    @Test
    void 消费不存在的refresh返回null() {
        when(valueOps.getAndDelete("rt:x")).thenReturn(null);

        assertThat(store.consumeRefresh("x")).isNull();
    }

    @Test
    void 删除refresh删对应键() {
        store.deleteRefresh("tok");

        verify(redis).delete("rt:tok");
    }

    @Test
    void 拉黑access写入bl键() {
        store.blacklistAccess("jti1", 100);

        verify(valueOps).set(eq("bl:jti1"), eq("1"), any(Duration.class));
    }

    @Test
    void 拉黑时jti为空或ttl非正则跳过() {
        store.blacklistAccess(null, 100);
        store.blacklistAccess("jti", 0);

        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void 黑名单命中返回true() {
        when(redis.hasKey("bl:jti1")).thenReturn(true);

        assertThat(store.isAccessBlacklisted("jti1")).isTrue();
    }

    @Test
    void jti为空视为未拉黑() {
        assertThat(store.isAccessBlacklisted(null)).isFalse();
    }
}
