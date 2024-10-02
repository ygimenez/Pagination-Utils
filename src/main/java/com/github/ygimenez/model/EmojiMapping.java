package com.github.ygimenez.model;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Effectively a {@link HashMap} with {@link Emoji} key and {@link Page} values, with built-in conversion.
 */
public class EmojiMapping<V> implements Mapping<V> {
	private final Map<ButtonId<?>, V> mapping = new LinkedHashMap<>();
	private final IdCursor<Emoji> cursor = new IdCursor<>();

	public Map<ButtonId<?>, V> toMap() {
		return mapping;
	}

	public int size() {
		return mapping.size();
	}

	public boolean isEmpty() {
		return mapping.isEmpty();
	}

	public boolean containsKey(Emoji key) {
		cursor.setId(key);
		return mapping.containsKey(cursor);
	}

	public boolean containsValue(V value) {
		return mapping.containsValue(value);
	}

	public V get(Emoji key) {
		cursor.setId(key);
		return mapping.get(cursor);
	}

	@Nullable
	public V put(Emoji key, V value) {
		return mapping.put(new EmojiId(key), value);
	}

	public V remove(Emoji key) {
		cursor.setId(key);
		return mapping.remove(cursor);
	}

	public void putAll(@NotNull Map<? extends Emoji, ? extends V> m) {
		m.forEach(this::put);
	}

	public void clear() {
		mapping.clear();
	}

	@NotNull
	public Set<ButtonId<?>> keySet() {
		return mapping.keySet();
	}

	@NotNull
	public Collection<V> values() {
		return mapping.values();
	}

	@NotNull
	public Set<Map.Entry<ButtonId<?>, V>> entrySet() {
		return mapping.entrySet();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EmojiMapping<?> that = (EmojiMapping<?>) o;
		return Objects.equals(mapping, that.mapping);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(mapping);
	}
}
