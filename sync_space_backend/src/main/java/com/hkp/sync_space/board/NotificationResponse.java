package com.hkp.sync_space.board;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
		UUID id,
		String kind,
		UUID userId,
		UUID spaceId,
		String spaceName,
		UUID taskId,
		String taskTitle,
		UUID actorId,
		String actorName,
		String body,
		Instant createdAt,
		boolean read) {
}
