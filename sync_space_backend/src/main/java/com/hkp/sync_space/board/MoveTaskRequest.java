package com.hkp.sync_space.board;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveTaskRequest(
		@NotNull(message = "Choose a column") UUID toColumnId,
		@NotNull(message = "Choose a position") @Min(value = 0, message = "Position cannot be negative") Integer toIndex) {
}
