package com.hkp.sync_space.auth;

import java.util.UUID;

public record UserResponse(UUID id, String email, String displayName) {
}
