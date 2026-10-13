package com.hkp.sync_space.space;

import com.hkp.sync_space.board.BoardResponse;

public record InviteOutcome(String status, BoardResponse board) {
}
