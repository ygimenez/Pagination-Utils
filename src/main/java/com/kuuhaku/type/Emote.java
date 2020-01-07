package com.kuuhaku.type;

public enum Emote {
	NEXT("\u25B6"), PREVIOUS("\u25C0"), ACCEPT("\u2705"), CANCEL("\u274E");

	private final String code;

	Emote(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
