package com.github.ygimenez.model;

import java.util.Objects;

/**
 * Subclass of {@link ButtonId} to represent text-only buttons.
 */
public class TextId implements ButtonId<String> {
	private final String id;

	/**
	 * Creates a new instance.
	 * @param id The {@link String} to be used
	 */
	public TextId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TextId textId = (TextId) o;
		return Objects.equals(id, textId.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
