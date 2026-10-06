package com.hkp.sync_space.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameColumnRequest(
		@NotBlank(message = "Column name is required") @Size(max = 80, message = "Column name must be 80 characters or fewer") String name) {
}
