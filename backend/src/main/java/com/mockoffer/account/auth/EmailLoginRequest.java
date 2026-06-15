package com.mockoffer.account.auth;

public record EmailLoginRequest(String email, String code) {
}
