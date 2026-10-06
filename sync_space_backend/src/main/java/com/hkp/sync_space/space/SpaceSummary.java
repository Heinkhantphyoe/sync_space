package com.hkp.sync_space.space;

import java.util.UUID;

public record SpaceSummary(UUID id, String name, String role, int memberCount) {
}
