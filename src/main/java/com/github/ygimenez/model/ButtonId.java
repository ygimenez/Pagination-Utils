package com.github.ygimenez.model;

import com.github.ygimenez.type.Emote;

/**
 * Internal interface for allowing both {@link String} and {@link Emoji} buttons to coexist.
 * @param <T> Type of this ID
 */
public interface ButtonId<T> {
	T getId();

	default String extractId() {
		if (this instanceof TextId) return ((TextId) this).getId();

		return Emote.getId(((EmojiId) this).getId());
	}
}
