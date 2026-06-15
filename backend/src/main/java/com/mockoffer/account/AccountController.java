package com.mockoffer.account;

import com.mockoffer.account.domain.UserIdentity;
import com.mockoffer.account.otp.OtpService;
import com.mockoffer.account.security.GitHubAuthenticationSuccessHandler;
import com.mockoffer.common.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 登录方式管理：列出、发起 GitHub 绑定、解绑、换绑邮箱。需登录态。 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService account;
    private final OtpService otp;
    private final boolean cookieSecure;

    public AccountController(AccountService account, OtpService otp,
            @Value("${app.cookie.secure:false}") boolean cookieSecure) {
        this.account = account;
        this.otp = otp;
        this.cookieSecure = cookieSecure;
    }

    @GetMapping("/identities")
    public ApiResponse<List<IdentityDetail>> list(@AuthenticationPrincipal Jwt principal) {
        long userId = Long.parseLong(principal.getSubject());
        List<IdentityDetail> list = account.listIdentities(userId).stream()
                .map(this::toDetail)
                .toList();
        return ApiResponse.ok(list);
    }

    /** 发起 GitHub 绑定：写入 intent cookie（当前 userId），重定向到授权端点；回调由 successHandler 完成绑定。 */
    @GetMapping("/identities/github/bind")
    public void bindGithub(@AuthenticationPrincipal Jwt principal, HttpServletResponse resp) throws IOException {
        Cookie c = new Cookie(GitHubAuthenticationSuccessHandler.BIND_COOKIE, principal.getSubject());
        c.setPath("/");
        c.setHttpOnly(true);
        c.setSecure(cookieSecure);
        c.setMaxAge(180);
        resp.addCookie(c);
        resp.sendRedirect("/oauth2/authorization/github");
    }

    /** 换绑邮箱第一步：对新邮箱发验证码。 */
    @PostMapping("/identities/email/code")
    public ApiResponse<Void> sendChangeEmailCode(@RequestBody ChangeEmailRequest req) {
        otp.sendCode(req.newEmail());
        return ApiResponse.ok(null);
    }

    /** 换绑邮箱第二步：校验新邮箱验证码后替换 / 绑定 email_otp 身份。 */
    @PutMapping("/identities/email")
    public ApiResponse<Void> changeEmail(@AuthenticationPrincipal Jwt principal, @RequestBody ChangeEmailRequest req) {
        otp.verify(req.newEmail(), req.code());
        account.changeEmail(Long.parseLong(principal.getSubject()), req.newEmail());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/identities/{provider}")
    public ApiResponse<Void> unbind(@AuthenticationPrincipal Jwt principal, @PathVariable String provider) {
        account.unbind(Long.parseLong(principal.getSubject()), provider);
        return ApiResponse.ok(null);
    }

    private IdentityDetail toDetail(UserIdentity i) {
        return new IdentityDetail(i.getProvider(), account.accountLabel(i), i.getLastUsedAt());
    }
}
