package com.mockoffer.account.domain;

/** 登录方式标识，与 user_identities.provider 取值一致。 */
public final class Provider {

    public static final String EMAIL_OTP = "email_otp";
    public static final String GITHUB = "github";
    // 预留：手机号 / 微信，本里程碑不实现，数据结构已兼容
    public static final String PHONE = "phone";
    public static final String WECHAT = "wechat";

    private Provider() {
    }
}
