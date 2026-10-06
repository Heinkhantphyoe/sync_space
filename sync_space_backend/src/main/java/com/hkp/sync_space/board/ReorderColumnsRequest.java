package com.hkp.sync_space.board;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record ReorderColumnsRequest(@NotEmpty(message = "Choose a column order") List<UUID> columnIds) {
}
