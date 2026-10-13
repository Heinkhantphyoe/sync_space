package com.hkp.sync_space.board;

import java.util.UUID;

public record CheckResponse(UUID id, String title, boolean done, int position) {
}
