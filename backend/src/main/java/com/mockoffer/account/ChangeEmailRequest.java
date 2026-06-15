package com.mockoffer.account;

public record ChangeEmailRequest(String newEmail, String code) {
}
