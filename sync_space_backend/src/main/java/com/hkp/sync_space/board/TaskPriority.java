package com.hkp.sync_space.board;

public enum TaskPriority {
	LOW("Low"),
	MEDIUM("Medium"),
	HIGH("High");

	private final String displayName;

	TaskPriority(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}

}
