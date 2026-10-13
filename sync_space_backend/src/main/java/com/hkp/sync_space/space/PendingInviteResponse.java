package com.hkp.sync_space.space;

import java.time.Instant;
import java.util.UUID;

public record PendingInviteResponse(UUID id, String email, Instant createdAt) {
}
