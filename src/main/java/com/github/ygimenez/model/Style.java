package com.github.ygimenez.model;

import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

public class Style {
	private final Emoji emoji;
	private final String label;
	private final ButtonStyle style;

	private Style(Emoji emoji, String label, ButtonStyle style) {
		this.emoji = emoji;
		this.label = label;
		this.style = style;
	}

	public static Style of(String emoji, String label, ButtonStyle style) {
		return new Style(Emoji.fromMarkdown(emoji), label, style);
	}

	public Emoji getEmoji() {
		return emoji;
	}

	public String getLabel() {
		return label;
	}

	public ButtonStyle getStyle() {
		return style;
	}
}
