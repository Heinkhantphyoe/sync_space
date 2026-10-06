package com.hkp.sync_space.auth;

public record AuthResponse(String token, UserResponse user) {
}
