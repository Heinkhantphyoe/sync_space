package com.hkp.sync_space.board;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
		UUID id,
		UUID columnId,
		String title,
		String description,
		int position,
		UUID createdBy,
		Instant updatedAt) {
}
