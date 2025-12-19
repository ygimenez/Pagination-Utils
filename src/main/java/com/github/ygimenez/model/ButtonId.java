package com.github.ygimenez.model;

import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.Nullable;

/**
 * Internal interface for allowing both {@link String} and {@link Emoji} buttons to coexist.
 * @param <T> Type of this ID
 */
public interface ButtonId<T> {
	/**
	 * Retrieves the value used to create this instance.
	 * @return The original value, either a {@link String} for {@link TextId} or an {@link Emoji} for {@link EmojiId}
	 */
	String getId();

	/**
	 * Retrieves the visual representation for this instance.
	 * @return The visual representation, either a {@link String} for {@link TextId} or an {@link Emoji} for {@link EmojiId}
	 */
	T getContent();

	/**
	 * Retrieves the label for this instance.
	 * @return The label for this instance, will be null if not declared during instantiation.
	 */
	@Nullable
	String getLabel();

	/**
	 * Retrieves the style of this button.
	 * @return The style of this button
	 */
	ButtonStyle getStyle();
}
