package com.hkp.sync_space.board;

public enum TaskLabel {
	BUG("Bug"),
	FEATURE("Feature"),
	DESIGN("Design");

	private final String displayName;

	TaskLabel(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}

}
