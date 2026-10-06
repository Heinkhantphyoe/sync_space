package com.hkp.sync_space.board;

import java.util.List;
import java.util.UUID;

public record ColumnResponse(UUID id, String name, int position, List<TaskResponse> tasks) {
}
