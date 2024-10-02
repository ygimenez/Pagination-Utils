package com.github.ygimenez.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Effectively a {@link HashMap} with {@link String} key and {@link Page} values, with built-in conversion.
 */
public class TextMapping<V> implements Mapping<V> {
	private final Map<ButtonId<?>, V> mapping = new LinkedHashMap<>();
	private final IdCursor<String> cursor = new IdCursor<>();

	public Map<ButtonId<?>, V> toMap() {
		return mapping;
	}

	public int size() {
		return mapping.size();
	}

	public boolean isEmpty() {
		return mapping.isEmpty();
	}

	public boolean containsKey(String key) {
		cursor.setId(key);
		return mapping.containsKey(cursor);
	}

	public boolean containsValue(V value) {
		return mapping.containsValue(value);
	}

	public V get(String key) {
		cursor.setId(key);
		return mapping.get(cursor);
	}

	@Nullable
	public V put(String key, V value) {
		return mapping.put(new TextId(key), value);
	}

	public V remove(String key) {
		cursor.setId(key);
		return mapping.remove(cursor);
	}

	public void putAll(@NotNull Map<? extends String, ? extends V> m) {
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
		TextMapping<?> that = (TextMapping<?>) o;
		return Objects.equals(mapping, that.mapping);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(mapping);
	}
}
