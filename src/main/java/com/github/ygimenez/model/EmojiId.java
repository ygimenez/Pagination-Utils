package com.github.ygimenez.model;

import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.Objects;

public class EmojiId implements ButtonId<Emoji> {
	private final Emoji id;

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
		if (o == null || getClass() != o.getClass()) return false;
		EmojiId emojiId = (EmojiId) o;
		return Objects.equals(id, emojiId.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
