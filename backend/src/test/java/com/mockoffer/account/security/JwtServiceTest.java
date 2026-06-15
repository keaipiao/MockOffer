package com.mockoffer.account.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** JwtService 单测：access 真签真验 round-trip；refresh 为不透明串并落库可吊销。 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock RefreshTokenStore store;

    JwtService jwt;
    JwtDecoder decoder;

    @BeforeEach
    void setup() {
        SecretKey key = new SecretKeySpec(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        jwt = new JwtService(encoder, store, 900, 2592000);
    }

    @Test
    void access令牌可被同密钥解码且带subject与jti() {
        Jwt decoded = decoder.decode(jwt.issueAccess(42L));

        assertThat(decoded.getSubject()).isEqualTo("42");
        assertThat(decoded.getId()).isNotBlank();
        assertThat(decoded.getExpiresAt()).isNotNull();
    }

    @Test
    void 每次签发的jti都不同() {
        Jwt a = decoder.decode(jwt.issueAccess(1L));
        Jwt b = decoder.decode(jwt.issueAccess(1L));

        assertThat(a.getId()).isNotEqualTo(b.getId());
    }

    @Test
    void refresh为不透明串并以refreshTtl落库() {
        String token = jwt.issueRefresh(7L);

        assertThat(token).isNotBlank();
        assertThat(token).doesNotContain("."); // 不是 JWT，是随机串
        verify(store).saveRefresh(token, 7L, 2592000);
    }

    @Test
    void 暴露配置的有效期() {
        assertThat(jwt.accessTtlSeconds()).isEqualTo(900);
        assertThat(jwt.refreshTtlSeconds()).isEqualTo(2592000);
    }
}
