package com.hkp.sync_space.board;

import java.time.Instant;
import java.util.UUID;

public record TaskActivityResponse(
		UUID id,
		String kind,
		UUID actorId,
		String actorName,
		String summary,
		String body,
		Instant createdAt) {
}
