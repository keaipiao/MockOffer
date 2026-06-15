package com.mockoffer.account.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.mockoffer.IntegrationTestBase;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** 账号体系金路径集成测试：真起 PG + Redis（基类提供），走完整 HTTP 鉴权链路。 */
class AuthFlowIntegrationTest extends IntegrationTestBase {

    @Value("${local.server.port}") int port;
    @Autowired StringRedisTemplate redisTemplate;

    RestClient client;

    @BeforeEach
    void setup() {
        client = RestClient.create("http://localhost:" + port);
    }

    @Test
    void 邮箱登录金路径_登录拿会话_me可用_登出后吊销401() {
        String email = "itest@example.com";

        assertThat(send("POST", "/api/auth/email/code", "{\"email\":\"" + email + "\"}", null).status())
                .isEqualTo(200);

        String code = redisTemplate.opsForValue().get("otp:code:" + email);
        assertThat(code).as("验证码应已写入 Redis").isNotNull();

        Resp login = send("POST", "/api/auth/email/login",
                "{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}", null);
        assertThat(login.status()).isEqualTo(200);
        String cookie = cookieHeader(login.setCookies());
        assertThat(cookie).contains("access_token");

        Resp me = send("GET", "/api/auth/me", null, cookie);
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.body()).contains("email_otp").contains(email);

        assertThat(send("POST", "/api/auth/logout", null, cookie).status()).isEqualTo(200);

        // 同一 access 已被拉黑 → 即时吊销
        assertThat(send("GET", "/api/auth/me", null, cookie).status()).isEqualTo(401);
    }

    @Test
    void 未登录访问受保护接口_401() {
        assertThat(send("GET", "/api/auth/me", null, null).status()).isEqualTo(401);
        assertThat(send("GET", "/api/account/identities", null, null).status()).isEqualTo(401);
    }

    private record Resp(int status, List<String> setCookies, String body) {}

    private Resp send(String method, String path, String jsonBody, String cookie) {
        RestClient.RequestBodySpec spec = client.method(HttpMethod.valueOf(method)).uri(path);
        if (cookie != null) {
            spec = spec.header(HttpHeaders.COOKIE, cookie);
        }
        if (jsonBody != null) {
            spec = spec.contentType(MediaType.APPLICATION_JSON).body(jsonBody);
        }
        return spec.exchange((req, res) -> new Resp(
                res.getStatusCode().value(),
                res.getHeaders().get(HttpHeaders.SET_COOKIE),
                new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8)), false);
    }

    /** 把登录响应的多条 Set-Cookie 折成一个 Cookie 请求头（只取 name=value 部分）。 */
    private static String cookieHeader(List<String> setCookies) {
        if (setCookies == null) {
            return "";
        }
        return setCookies.stream().map(c -> c.split(";", 2)[0]).collect(Collectors.joining("; "));
    }
}
