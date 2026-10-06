package com.hkp.sync_space.board;

import java.util.List;
import java.util.UUID;

public record BoardState(
		UUID spaceId,
		String spaceName,
		long revision,
		List<ColumnResponse> columns,
		List<MemberResponse> members) {
}
