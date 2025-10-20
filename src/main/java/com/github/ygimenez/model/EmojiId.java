package com.github.ygimenez.model;

import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Subclass of {@link ButtonId} to represent {@link Emoji}-only buttons.
 */
public class EmojiId implements ButtonId<Emoji> {
	private final Emoji id;
	private final String label;
	private final ButtonStyle style;

	/**
	 * Creates a new instance.
	 * @param id The {@link Emoji} to be used.
	 */
	public EmojiId(@NotNull Emoji id) {
		this(id, ButtonStyle.SECONDARY);
	}

	/**
	 * Creates a new instance with a chosen style.
	 * @param id The {@link Emoji} to be used.
	 * @param label The label to be used.
	 */
	public EmojiId(@NotNull Emoji id, String label) {
		this(id, label, ButtonStyle.SECONDARY);
	}

	/**
	 * Creates a new instance with a chosen style.
	 * @param id The {@link Emoji} to be used.
	 * @param style The {@link ButtonStyle} to be used.
	 */
	public EmojiId(@NotNull Emoji id, @NotNull ButtonStyle style) {
		this(id, null, style);
	}

	/**
	 * Creates a new instance with a label and chosen style.
	 * @param id The {@link Emoji} to be used.
	 * @param label The label to be used, can be null.
	 * @param style The {@link ButtonStyle} to be used.
	 */
	public EmojiId(@NotNull Emoji id, String label, @NotNull ButtonStyle style) {
		this.id = id;
		this.label = label;
		this.style = style;
	}

	@Override
	public Emoji getId() {
		return id;
	}

	@Nullable
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public ButtonStyle getStyle() {
		return style;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ButtonId<?>)) return false;
		ButtonId<?> textId = (ButtonId<?>) o;
		return Objects.equals(id, textId.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
