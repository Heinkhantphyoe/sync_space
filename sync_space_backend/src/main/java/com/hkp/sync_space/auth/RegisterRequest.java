package com.hkp.sync_space.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "Email is required") @Email(message = "Enter a valid email") @Size(max = 255) String email,
		@NotBlank(message = "Password is required") @Size(min = 8, max = 100, message = "Password must be at least 8 characters") String password,
		@NotBlank(message = "Name is required") @Size(max = 80, message = "Name must be 80 characters or fewer") String displayName) {
}
