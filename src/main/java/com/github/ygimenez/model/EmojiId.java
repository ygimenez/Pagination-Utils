package com.github.ygimenez.model;

import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.Objects;

/**
 * Subclass of {@link ButtonId} to represent {@link Emoji}-only buttons.
 */
public class EmojiId implements ButtonId<Emoji> {
	private final Emoji id;

	/**
	 * Creates a new instance.
	 * @param id The {@link Emoji} to be used
	 */
	public EmojiId(Emoji id) {
		this.id = id;
	}

	@Override
	public Emoji getId() {
		return id;
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
