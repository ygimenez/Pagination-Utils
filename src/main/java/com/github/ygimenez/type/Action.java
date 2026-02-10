package com.github.ygimenez.type;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ButtonId;
import com.github.ygimenez.model.TextId;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Enumerator representing values required by non-dynamic buttons.
 */
public enum Action {
	/**
	 * {@link Action} representing the "next" button (default: ▶).
	 */
	NEXT(Emoji.fromUnicode("▶"), ButtonStyle.SECONDARY),
	/**
	 * {@link Action} representing the "previous" button (default: ◀).
	 */
	PREVIOUS(Emoji.fromUnicode("◀"), ButtonStyle.SECONDARY),
	/**
	 * {@link Action} representing the "accept" button (default: ✅).
	 */
	ACCEPT(Emoji.fromUnicode("✅"), ButtonStyle.SUCCESS),
	/**
	 * {@link Action} representing the "cancel" button (default: ❎).
	 */
	CANCEL(Emoji.fromUnicode("❎"), ButtonStyle.DANGER),
	/**
	 * {@link Action} representing the "skip forward" button (default: ⏩).
	 */
	SKIP_FORWARD(Emoji.fromUnicode("⏩"), ButtonStyle.SECONDARY),
	/**
	 * {@link Action} representing the "skip backward" button (default: ⏪).
	 */
	SKIP_BACKWARD(Emoji.fromUnicode("⏪"), ButtonStyle.SECONDARY),
	/**
	 * {@link Action} representing the "go to first" button (default: ⏮).
	 */
	GOTO_FIRST(Emoji.fromUnicode("⏮"), ButtonStyle.SECONDARY),
	/**
	 * {@link Action} representing the "go to last" button (default: ⏭).
	 */
	GOTO_LAST(Emoji.fromUnicode("⏭"), ButtonStyle.SECONDARY),
	/**
	 * {@link Action} representing nothing.
	 */
	NONE(null, ButtonStyle.SECONDARY);

	private final Emoji emj;
	private final ButtonStyle style;

	Action(Emoji emj, ButtonStyle style) {
		this.emj = emj;
		this.style = style;
	}

	/**
	 * Retrieves this {@link Action}'s {@link Emoji}.
	 *
	 * @return This {@link Action}'s {@link Emoji}.
	 */
	public Emoji getEmoji() {
		return emj;
	}

	/**
	 * Retrieves this {@link Action}'s {@link ButtonStyle}.
	 *
	 * @return This {@link Action}'s {@link ButtonStyle}.
	 */
	public ButtonStyle getStyle() {
		return style;
	}

	/**
	 * Retrieves the {@link Action} linked to supplied {@link Emoji}.
	 *
	 * @param emoji The {@link Emoji} to be searched for.
	 * @return The respective {@link Action}, or {@link #NONE} if it didn't match any.
	 */
	public static Action getByEmoji(@NotNull Emoji emoji) {
		for (Map.Entry<Action, Emoji> entry : Pages.getPaginator().getEmotes().entrySet()) {
			if (Objects.equals(entry.getValue(), emoji)) return entry.getKey();
		}

		for (Action emt : values()) {
			if (Objects.equals(emt.emj, emoji)) return emt;
		}

		return NONE;
	}

	/**
	 * Checks whether the supplied {@link Button} is referenced by a library emote or not.
	 *
	 * @param btn The {@link Button} to be checked.
	 * @return Whether it uses a {@link Action} value or not.
	 */
	public static boolean isNative(@NotNull Button btn) {
		if (btn.getCustomId() == null) return false;
		String id = ButtonId.ID_PATTERN.split(btn.getCustomId())[0];

		for (Action emt : values()) {
			if (emt.name().equals(id)) return true;
		}

		return false;
	}

	/**
	 * Checks whether the supplied {@link MessageReaction} is referenced by a library emote or not.
	 *
	 * @param react The {@link MessageReaction} to be checked.
	 * @return Whether it uses a {@link Action} value or not.
	 */
	public static boolean isNative(@NotNull MessageReaction react) {
		Emoji emj = Emoji.fromFormatted(react.getEmoji().getAsReactionCode());
		for (Action emt : values()) {
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
	public static String getId(@NotNull Emoji emj) {
		if (emj instanceof CustomEmoji) {
			return ((CustomEmoji) emj).getId();
		} else {
			return emj.getName();
		}
	}

	/**
	 * Returns the {@link Action} represented by the supplied {@link Button}, if any.
	 *
	 * @param btn The {@link Button} to be checked.
	 * @return The {@link Action} linked to the supplied {@link Button}, or null if none.
	 */
	@Nullable
	public static Action fromButton(@NotNull Button btn) {
		return Arrays.stream(values())
				.filter(e -> btn.getCustomId() != null && btn.getCustomId().contains(e.name()))
				.findFirst().orElse(null);
	}
}
