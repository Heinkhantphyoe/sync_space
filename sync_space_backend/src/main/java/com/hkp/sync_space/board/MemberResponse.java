package com.hkp.sync_space.board;

import java.util.List;
import java.util.UUID;

public record MemberResponse(UUID userId, String displayName, String email, String role) {
}
