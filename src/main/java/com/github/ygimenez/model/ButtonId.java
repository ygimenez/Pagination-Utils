package com.github.ygimenez.model;

import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.entities.emoji.Emoji;

/**
 * Internal interface for allowing both {@link String} and {@link Emoji} buttons to coexist.
 * @param <T> Type of this ID
 */
public interface ButtonId<T> {
	/**
	 * Retrieves the value used to instantiate this object.
	 * @return The original object, either a {@link String} for {@link TextId} or an {@link Emoji} for {@link EmojiId}
	 */
	T getId();

	/**
	 * Converts the value into a {@link String} representation for usage in pagination handlers.
	 * @return The original object, converted into its {@link String} representation
	 */
	default String extractId() {
		if (this instanceof TextId) return ((TextId) this).getId();

		return Emote.getId(((EmojiId) this).getId());
	}
}
