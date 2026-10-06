package com.hkp.sync_space.auth;

import java.util.UUID;

public record AuthUser(UUID id, String email, String displayName) {
}
