package com.github.ygimenez.type;

public enum Emote {
	NEXT("\u25B6"), PREVIOUS("\u25C0"), ACCEPT("\u2705"), CANCEL("\u274E"), SKIP_FORWARD("\u23E9"), SKIP_BACKWARD("\u23EA");

	private final String code;

	Emote(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
