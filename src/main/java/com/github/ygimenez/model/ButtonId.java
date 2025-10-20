package com.github.ygimenez.model;

import com.github.ygimenez.type.Action;
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
	T getId();

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

	/**
	 * Converts the value into a {@link String} representation for usage in pagination handlers.
	 * @return The original value, converted into its {@link String} representation
	 */
	default String extractId() {
		if (this instanceof TextId) return ((TextId) this).getId();

		return Action.getId(((EmojiId) this).getId());
	}
}
