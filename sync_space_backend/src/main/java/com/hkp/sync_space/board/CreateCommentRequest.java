package com.hkp.sync_space.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
		@NotBlank(message = "Comment is required") @Size(max = 2000, message = "Comment must be 2000 characters or fewer") String body) {
}
