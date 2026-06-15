package com.mockoffer.account.security;

import tools.jackson.databind.ObjectMapper;
import com.mockoffer.account.AccountService;
import com.mockoffer.account.domain.Provider;
import com.mockoffer.common.BizException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * GitHub 授权回调成功：按 intent cookie 区分「登录」与「绑定到当前用户」，
 * 用 GitHub numeric id 归一/绑定，签发自有 JWT cookie，再重定向回前端白名单地址。
 */
@Component
public class GitHubAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    /** 绑定发起时由 AccountController 写入，值为当前登录 userId；登录场景该 cookie 不存在。 */
    public static final String BIND_COOKIE = "oauth_bind_uid";

    private final AccountService account;
    private final JwtService jwt;
    private final TokenCookieManager cookie;
    private final ObjectMapper objectMapper;
    private final String frontend;
    private final boolean secure;

    public GitHubAuthenticationSuccessHandler(AccountService account, JwtService jwt,
            TokenCookieManager cookie, ObjectMapper objectMapper,
            @Value("${app.frontend-base-url}") String frontend,
            @Value("${app.cookie.secure:false}") boolean secure) {
        this.account = account;
        this.jwt = jwt;
        this.cookie = cookie;
        this.objectMapper = objectMapper;
        this.frontend = frontend;
        this.secure = secure;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        OAuth2User user = (OAuth2User) authentication.getPrincipal();
        String githubId = String.valueOf(user.<Object>getAttribute("id"));
        String meta = buildMeta(user);
        String bindUid = readBindCookie(request);
        clearBindCookie(response);

        try {
            if (bindUid != null) {
                account.bindIdentity(Long.parseLong(bindUid), Provider.GITHUB, githubId, meta);
                response.sendRedirect(frontend + "/account/settings?bind=github&status=ok");
            } else {
                long userId = account.loginOrRegister(Provider.GITHUB, githubId, meta);
                cookie.writeAccess(response, jwt.issueAccess(userId), jwt.accessTtlSeconds());
                cookie.writeRefresh(response, jwt.issueRefresh(userId), jwt.refreshTtlSeconds());
                response.sendRedirect(frontend + "/");
            }
        } catch (BizException e) {
            String scene = bindUid != null ? "/account/settings?bind=github" : "/login?error";
            response.sendRedirect(frontend + scene + "&status=fail&code=" + e.getCode());
        }
    }

    private String buildMeta(OAuth2User user) {
        Map<String, Object> meta = new LinkedHashMap<>();
        putIfPresent(meta, "login", user.getAttribute("login"));
        putIfPresent(meta, "avatar", user.getAttribute("avatar_url"));
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            return null;
        }
    }

    private void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private String readBindCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (BIND_COOKIE.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    private void clearBindCookie(HttpServletResponse response) {
        Cookie c = new Cookie(BIND_COOKIE, "");
        c.setPath("/");
        c.setHttpOnly(true);
        c.setSecure(secure);
        c.setMaxAge(0);
        response.addCookie(c);
    }
}
