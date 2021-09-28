package com.github.ygimenez.type;

import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

/**
 * Enumerator representing values required by non-dynamic buttons.
 */
public enum Emote {
	/**
	 * {@link Emote} representing the "next" button (default: ▶).
	 */
	NEXT(Emoji.fromUnicode("▶"), ButtonStyle.SECONDARY),
	/**
	 * {@link Emote} representing the "previous" button (default: ◀).
	 */
	PREVIOUS(Emoji.fromUnicode("◀"), ButtonStyle.SECONDARY),
	/**
	 * {@link Emote} representing the "accept" button (default: ✅).
	 */
	ACCEPT(Emoji.fromUnicode("✅"), ButtonStyle.SUCCESS),
	/**
	 * {@link Emote} representing the "cancel" button (default: ❎).
	 */
	CANCEL(Emoji.fromUnicode("❎"), ButtonStyle.DANGER),
	/**
	 * {@link Emote} representing the "skip forward" button (default: ⏩).
	 */
	SKIP_FORWARD(Emoji.fromUnicode("⏩"), ButtonStyle.SECONDARY),
	/**
	 * {@link Emote} representing the "skip backward" button (default: ⏪).
	 */
	SKIP_BACKWARD(Emoji.fromUnicode("⏪"), ButtonStyle.SECONDARY),
	/**
	 * {@link Emote} representing the "go to first" button (default: ⏮).
	 */
	GOTO_FIRST(Emoji.fromUnicode("⏮"), ButtonStyle.SECONDARY),
	/**
	 * {@link Emote} representing the "go to last" button (default: ⏭).
	 */
	GOTO_LAST(Emoji.fromUnicode("⏭"), ButtonStyle.SECONDARY),
	/**
	 * {@link Emote} representing nothing.
	 */
	NONE(null, ButtonStyle.SECONDARY);

	private final Emoji emj;
	private final ButtonStyle style;

	Emote(Emoji emj, ButtonStyle style) {
		this.emj = emj;
		this.style = style;
	}

	public Emoji getDefault() {
		return emj;
	}

	public ButtonStyle getStyle() {
		return style;
	}

	public static Emote getByEmoji(Emoji emoji) {
		for (Emote emt : values()) {
			if (emt.emj.equals(emoji)) return emt;
		}

		return NONE;
	}
}
