package com.mockoffer.account.security;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** 签发 access（短期 JWT，带 jti）与 refresh（不透明随机串，存 Redis 可吊销）。 */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final RefreshTokenStore store;
    private final long accessTtl;
    private final long refreshTtl;
    private final SecureRandom random = new SecureRandom();

    public JwtService(JwtEncoder encoder, RefreshTokenStore store,
            @Value("${app.jwt.access-ttl-seconds}") long accessTtl,
            @Value("${app.jwt.refresh-ttl-seconds}") long refreshTtl) {
        this.encoder = encoder;
        this.store = store;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public String issueAccess(long userId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(Long.toString(userId))
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTtl))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public String issueRefresh(long userId) {
        byte[] buf = new byte[32];
        random.nextBytes(buf);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        store.saveRefresh(token, userId, refreshTtl);
        return token;
    }

    public long accessTtlSeconds() {
        return accessTtl;
    }

    public long refreshTtlSeconds() {
        return refreshTtl;
    }
}
