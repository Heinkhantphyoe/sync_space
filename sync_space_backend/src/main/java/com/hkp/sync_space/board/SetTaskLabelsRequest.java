package com.hkp.sync_space.board;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record SetTaskLabelsRequest(@NotNull(message = "Labels are required") List<TaskLabel> labels) {
}
