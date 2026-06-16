package com.mockoffer.account.auth;

import com.mockoffer.account.AccountService;
import com.mockoffer.account.domain.Provider;
import com.mockoffer.account.otp.OtpService;
import com.mockoffer.account.repo.UserIdentityRepository;
import com.mockoffer.account.security.JwtService;
import com.mockoffer.account.security.RefreshTokenStore;
import com.mockoffer.account.security.TokenCookieManager;
import com.mockoffer.common.ApiResponse;
import com.mockoffer.common.BizException;
import com.mockoffer.common.Emails;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证与会话：发码、验证码登录注册、当前用户、登出、刷新。 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** 旧 refresh 轮换后的宽限期（秒）：容忍前端并发刷新，过后失效。 */
    private static final long REFRESH_GRACE_SECONDS = 30;

    private final OtpService otp;
    private final AccountService account;
    private final JwtService jwt;
    private final TokenCookieManager cookie;
    private final RefreshTokenStore store;
    private final UserIdentityRepository identities;

    public AuthController(OtpService otp, AccountService account, JwtService jwt,
            TokenCookieManager cookie, RefreshTokenStore store, UserIdentityRepository identities) {
        this.otp = otp;
        this.account = account;
        this.jwt = jwt;
        this.cookie = cookie;
        this.store = store;
        this.identities = identities;
    }

    @PostMapping("/email/code")
    public ApiResponse<Void> sendCode(@RequestBody SendCodeRequest req) {
        otp.sendCode(req.email());
        return ApiResponse.ok(null);
    }

    @PostMapping("/email/login")
    public ApiResponse<MeResponse> emailLogin(@RequestBody EmailLoginRequest req, HttpServletResponse resp) {
        otp.verify(req.email(), req.code());
        String email = Emails.normalize(req.email());
        long userId = account.loginOrRegister(Provider.EMAIL_OTP, email, null);
        issueSession(resp, userId);
        return ApiResponse.ok(buildMe(userId));
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal Jwt principal) {
        long userId = Long.parseLong(principal.getSubject());
        return ApiResponse.ok(buildMe(userId));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal Jwt principal,
            @CookieValue(name = TokenCookieManager.REFRESH_COOKIE, required = false) String refresh,
            HttpServletResponse resp) {
        if (principal != null) {
            store.blacklistAccess(principal.getId(), secondsUntil(principal.getExpiresAt()));
        }
        if (refresh != null) {
            store.deleteRefresh(refresh);
        }
        cookie.clear(resp);
        return ApiResponse.ok(null);
    }

    @PostMapping("/refresh")
    public ApiResponse<Void> refresh(
            @CookieValue(name = TokenCookieManager.REFRESH_COOKIE, required = false) String refresh,
            HttpServletResponse resp) {
        if (refresh == null) {
            throw new BizException(40101, "未登录或会话已过期");
        }
        Long userId = store.rotateRefresh(refresh, REFRESH_GRACE_SECONDS);
        if (userId == null) {
            throw new BizException(40101, "会话已过期，请重新登录");
        }
        issueSession(resp, userId);
        return ApiResponse.ok(null);
    }

    private void issueSession(HttpServletResponse resp, long userId) {
        cookie.writeAccess(resp, jwt.issueAccess(userId), jwt.accessTtlSeconds());
        cookie.writeRefresh(resp, jwt.issueRefresh(userId), jwt.refreshTtlSeconds());
    }

    private MeResponse buildMe(long userId) {
        var list = identities.findByUserIdAndDeletedAtIsNull(userId).stream()
                .map(i -> new IdentitySummary(i.getProvider(), account.accountLabel(i), i.getLastUsedAt()))
                .toList();
        return new MeResponse(userId, list);
    }

    private long secondsUntil(Instant expiresAt) {
        if (expiresAt == null) {
            return 0;
        }
        return Math.max(expiresAt.getEpochSecond() - Instant.now().getEpochSecond(), 0);
    }
}
