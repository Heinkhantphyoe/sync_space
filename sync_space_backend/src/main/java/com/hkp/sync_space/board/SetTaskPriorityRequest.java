package com.hkp.sync_space.board;

import jakarta.validation.constraints.NotNull;

public record SetTaskPriorityRequest(@NotNull(message = "Priority is required") TaskPriority priority) {
}
