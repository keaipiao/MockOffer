package com.mockoffer.account.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * 把 OAuth2 授权请求存进短期 httpOnly cookie，而非 session——无状态（STATELESS）下 oauth2Login 必需。
 * 反序列化用 ObjectInputFilter 白名单限制类，防止伪造 cookie 触发反序列化攻击。
 */
@Component
public class CookieAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE = "oauth2_auth_request";
    private static final int EXPIRE_SECONDS = 180;
    private static final ObjectInputFilter FILTER = ObjectInputFilter.Config.createFilter(
            "org.springframework.security.oauth2.**;java.util.**;java.lang.**;java.time.**;java.net.**;!*");

    private final boolean secure;

    public CookieAuthorizationRequestRepository(@Value("${app.cookie.secure:false}") boolean secure) {
        this.secure = secure;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie c = read(request);
        return c == null ? null : deserialize(c.getValue());
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            clear(response);
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(COOKIE, serialize(authorizationRequest))
                .path("/").httpOnly(true).secure(secure).sameSite("Lax").maxAge(EXPIRE_SECONDS).build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
            HttpServletResponse response) {
        OAuth2AuthorizationRequest req = loadAuthorizationRequest(request);
        if (req != null) {
            clear(response);
        }
        return req;
    }

    private Cookie read(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (COOKIE.equals(c.getName())) {
                    return c;
                }
            }
        }
        return null;
    }

    private void clear(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE, "")
                .path("/").httpOnly(true).secure(secure).sameSite("Lax").maxAge(0).build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String serialize(OAuth2AuthorizationRequest req) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(req);
            return Base64.getUrlEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("序列化授权请求失败", e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            byte[] data = Base64.getUrlDecoder().decode(value);
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
                ois.setObjectInputFilter(FILTER);
                return (OAuth2AuthorizationRequest) ois.readObject();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
