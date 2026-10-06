package com.hkp.sync_space.board;

import java.util.UUID;

public record BoardChanged(String type, UUID spaceId, BoardState board) {
}
