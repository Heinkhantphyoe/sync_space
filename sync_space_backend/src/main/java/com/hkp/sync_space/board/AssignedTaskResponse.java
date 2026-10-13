package com.hkp.sync_space.board;

import java.time.LocalDate;
import java.util.UUID;

public record AssignedTaskResponse(
		UUID id,
		String title,
		TaskPriority priority,
		LocalDate dueOn,
		UUID spaceId,
		String spaceName,
		String columnName) {
}
