package com.hkp.sync_space.board;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
		@NotNull(message = "Choose a column") UUID columnId,
		@NotBlank(message = "Title is required") @Size(max = 200, message = "Title must be 200 characters or fewer") String title,
		@Size(max = 4000, message = "Description must be 4000 characters or fewer") String description) {
}
