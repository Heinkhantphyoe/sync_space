package com.hkp.sync_space.space;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteMemberRequest(
		@NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email) {
}
