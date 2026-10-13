package com.hkp.sync_space.board;

import jakarta.validation.constraints.Size;

public record UpdateCheckRequest(
		@Size(max = 200, message = "Checklist item must be 200 characters or fewer") String title,
		Boolean done) {
}
