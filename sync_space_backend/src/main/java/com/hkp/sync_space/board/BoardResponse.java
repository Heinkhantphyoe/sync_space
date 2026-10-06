package com.hkp.sync_space.board;

import java.util.List;
import java.util.UUID;

public record BoardResponse(
		UUID spaceId,
		String spaceName,
		long revision,
		String role,
		List<ColumnResponse> columns,
		List<MemberResponse> members) {
}
