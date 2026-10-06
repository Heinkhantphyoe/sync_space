package com.hkp.sync_space.board;

import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
		@Size(max = 200, message = "Title must be 200 characters or fewer") String title,
		@Size(max = 4000, message = "Description must be 4000 characters or fewer") String description) {
}
