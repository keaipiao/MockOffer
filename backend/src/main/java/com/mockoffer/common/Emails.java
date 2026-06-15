package com.mockoffer.common;

/** 邮箱规范化与基础校验：登录归一、Redis key、唯一约束统一用规范化后的值。 */
public final class Emails {

    private Emails() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public static boolean isValid(String email) {
        return email != null && email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
}
