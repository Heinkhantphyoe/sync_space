package com.hkp.sync_space.board;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AssignTaskRequest(@NotNull(message = "Assignees are required") List<UUID> userIds) {
}
