package com.github.ygimenez.type;

import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

import java.util.Objects;

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

	/**
	 * Retrieves this {@link Emote}'s default {@link Emoji}.
	 *
	 * @return This {@link Emote}'s default {@link Emoji}.
	 */
	public Emoji getDefault() {
		return emj;
	}

	/**
	 * Retrieves this {@link Emote}'s {@link ButtonStyle}.
	 *
	 * @return This {@link Emote}'s {@link ButtonStyle}.
	 */
	public ButtonStyle getStyle() {
		return style;
	}

	/**
	 * Retrieves the {@link Emote} linked to supplied {@link Emoji}.
	 *
	 * @param emoji The {@link Emoji} to be searched for.
	 * @return The respective {@link Emote}, or {@link #NONE} if it didn't match any.
	 */
	public static Emote getByEmoji(Emoji emoji) {
		for (Emote emt : values()) {
			if (Objects.equals(emt.emj, emoji)) return emt;
		}

		return NONE;
	}

	/**
	 * Checks whether the supplied {@link Button} is referenced by a library native emote or not.
	 *
	 * @param btn The {@link Button} to be checked.
	 * @return Whether it uses a {@link Emote} value or not.
	 */
	public static boolean isNative(Button btn) {
		if (btn.getId() == null) return false;
		else for (Emote emt : values()) {
			if (emt.name().equals(btn.getId())) return true;
		}

		return false;
	}

	/**
	 * Checks whether the supplied {@link MessageReaction} is referenced by a library native emote or not.
	 *
	 * @param react The {@link MessageReaction} to be checked.
	 * @return Whether it uses a {@link Emote} value or not.
	 */
	public static boolean isNative(MessageReaction react) {
		Emoji emj = Emoji.fromMarkdown(react.getReactionEmote().getAsReactionCode());
		for (Emote emt : values()) {
			if (emt.emj.equals(emj)) return false;
		}

		return true;
	}

	/**
	 * Utility method for retrieving an {@link Emoji}'s effective ID.
	 *
	 * @param emj The {@link Emoji} to be used.
	 * @return The supplied {@link Emoji}'s effective ID.
	 */
	public static String getId(Emoji emj) {
			return emj.isCustom() ? emj.getId() : emj.getName();
	}
}
