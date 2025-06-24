package com.github.ygimenez.model;

import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Subclass of {@link ButtonId} to act as a cursor for other mappers. Shouldn't be used outside the library.
 * @param <T> The type of the cursor
 */
class IdCursor<T> implements ButtonId<T> {
	private T id;

	@Override
	public T getId() {
		return id;
	}

	@Nullable
	@Override
	public String getLabel() {
		return null;
	}

	@Override
	public ButtonStyle getStyle() {
		return ButtonStyle.SECONDARY;
	}

	public void setId(T id) {
		this.id = id;
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
