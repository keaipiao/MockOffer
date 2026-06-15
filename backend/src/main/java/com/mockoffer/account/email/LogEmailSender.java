package com.mockoffer.account.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * dev 默认实现：把验证码打到日志，便于本地联调，不依赖 SES 凭证。
 * prod 接入腾讯云 SES 时新增 SesEmailSender（@Profile("prod") + @Primary）替代。
 */
@Component
public class LogEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

    @Override
    public void sendOtp(String email, String code) {
        log.info("[DEV 验证码] 发往 {} 的验证码：{}（dev 用日志代发，prod 接腾讯云 SES）", email, code);
    }
}
