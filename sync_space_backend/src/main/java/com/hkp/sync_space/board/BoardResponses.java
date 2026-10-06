package com.hkp.sync_space.board;

import com.hkp.sync_space.space.MemberRole;

public final class BoardResponses {

	private BoardResponses() {
	}

	public static BoardResponse from(BoardState state, MemberRole role) {
		return new BoardResponse(state.spaceId(), state.spaceName(), state.revision(), role.name(), state.columns(),
				state.members());
	}

}
