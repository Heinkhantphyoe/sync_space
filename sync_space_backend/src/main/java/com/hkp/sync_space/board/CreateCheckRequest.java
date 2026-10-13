package com.hkp.sync_space.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCheckRequest(
		@NotBlank(message = "Checklist item is required") @Size(max = 200, message = "Checklist item must be 200 characters or fewer") String title) {
}
