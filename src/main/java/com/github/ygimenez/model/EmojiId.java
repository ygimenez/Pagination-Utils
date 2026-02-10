package com.github.ygimenez.model;

import com.github.ygimenez.type.Action;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Subclass of {@link ButtonId} to represent {@link Emoji}-only buttons.
 */
public class EmojiId implements ButtonId<Emoji> {
	private final String id;
	private final Emoji content;
	private final String label;
	private final ButtonStyle style;

	/**
	 * Creates a new instance.
	 * @param content The {@link Emoji} to be used.
	 */
	public EmojiId(@NotNull Emoji content) {
		this(content, ButtonStyle.SECONDARY);
	}

	/**
	 * Creates a new instance with a chosen style.
	 * @param content The {@link Emoji} to be used.
	 * @param label The label to be used.
	 */
	public EmojiId(@NotNull Emoji content, String label) {
		this(content, label, ButtonStyle.SECONDARY);
	}

	/**
	 * Creates a new instance with a chosen style.
	 * @param content The {@link Emoji} to be used.
	 * @param style The {@link ButtonStyle} to be used.
	 */
	public EmojiId(@NotNull Emoji content, @NotNull ButtonStyle style) {
		this(content, null, style);
	}

	/**
	 * Creates a new instance with a label and chosen style.
	 * @param content The {@link Emoji} to be used.
	 * @param label The label to be used, can be null.
	 * @param style The {@link ButtonStyle} to be used.
	 */
	public EmojiId(@NotNull Emoji content, String label, @NotNull ButtonStyle style) {
		this(Action.getId(content), content, label, style);
	}

	/**
	 * Creates a new instance with a specific id, label, and chosen style.
	 * @param id The ID to represent this button.
	 * @param content The {@link Emoji} to be used.
	 * @param label The label to be used, can be null.
	 * @param style The {@link ButtonStyle} to be used.
	 */
	public EmojiId(@NotNull String id, @NotNull Emoji content, String label, @NotNull ButtonStyle style) {
		this.id = id;
		this.content = content;
		this.label = label;
		this.style = style;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Emoji getContent() {
		return content;
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
