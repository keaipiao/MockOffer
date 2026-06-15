package com.mockoffer.account.auth;

import java.time.OffsetDateTime;

/** 登录方式摘要：account 为展示用账号标识（邮箱地址 / GitHub @用户名），由 AccountService.accountLabel 统一计算。 */
public record IdentitySummary(String provider, String account, OffsetDateTime lastUsedAt) {
}
