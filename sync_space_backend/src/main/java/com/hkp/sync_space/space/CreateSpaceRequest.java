package com.hkp.sync_space.space;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSpaceRequest(
		@NotBlank(message = "Space name is required") @Size(max = 80, message = "Space name must be 80 characters or fewer") String name) {
}
