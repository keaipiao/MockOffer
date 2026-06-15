package com.mockoffer.account.auth;

import java.util.List;

public record MeResponse(long userId, List<IdentitySummary> identities) {
}
