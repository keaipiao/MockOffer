package com.mockoffer.account;

import java.time.OffsetDateTime;

public record IdentityDetail(String provider, String account, OffsetDateTime lastUsedAt) {
}
