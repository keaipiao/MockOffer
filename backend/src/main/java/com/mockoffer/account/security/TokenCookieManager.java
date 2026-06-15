package com.mockoffer.account.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** 把 access / refresh 写进 httpOnly cookie，或清除。SameSite=Lax 配合 CORS 防 CSRF。 */
@Component
public class TokenCookieManager {

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";

    private final boolean secure;

    public TokenCookieManager(@Value("${app.cookie.secure:false}") boolean secure) {
        this.secure = secure;
    }

    public void writeAccess(HttpServletResponse resp, String token, long maxAgeSeconds) {
        add(resp, ACCESS_COOKIE, token, maxAgeSeconds);
    }

    public void writeRefresh(HttpServletResponse resp, String token, long maxAgeSeconds) {
        add(resp, REFRESH_COOKIE, token, maxAgeSeconds);
    }

    public void clear(HttpServletResponse resp) {
        add(resp, ACCESS_COOKIE, "", 0);
        add(resp, REFRESH_COOKIE, "", 0);
    }

    private void add(HttpServletResponse resp, String name, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
        resp.addHeader("Set-Cookie", cookie.toString());
    }
}
