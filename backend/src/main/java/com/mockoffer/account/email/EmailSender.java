package com.mockoffer.account.email;

/** 验证码邮件发送抽象：dev 用日志代发，prod 接腾讯云 SES（凭证 + 审核模板就绪后实现）。 */
public interface EmailSender {

    void sendOtp(String email, String code);
}
