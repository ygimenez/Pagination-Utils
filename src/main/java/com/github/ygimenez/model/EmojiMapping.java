package com.github.ygimenez.model;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Effectively a {@link HashMap} with {@link Emoji} key and {@link Page} values, with built-in conversion.
 */
public class EmojiMapping<V> implements Mapping<V> {
	private final Map<ButtonId<?>, V> mapping;
	private final IdCursor<Emoji> cursor = new IdCursor<>();

	/**
	 * Creates a new instance, with default map implementation.
	 */
	public EmojiMapping() {
		this(new LinkedHashMap<>());
	}

	/**
	 * Creates a new instance, using supplied map.
	 *
	 * @param mapping The map to be used internally to store mappings.
	 */
	public EmojiMapping(Map<ButtonId<?>, V> mapping) {
		this.mapping = mapping;
	}

	/**
	 * Retrieves the underlying map containing the actual entries.
	 *
	 * @return The map containing the actual entries
	 */
	public Map<ButtonId<?>, V> toMap() {
		return mapping;
	}

	/**
	 * Returns the number of key-value mappings in this map.
	 *
	 * @return the number of key-value mappings in this map
	 */
	public int size() {
		return mapping.size();
	}

	/**
	 * Returns {@code true} if this map contains no key-value mappings.
	 *
	 * @return {@code true} if this map contains no key-value mappings
	 */
	public boolean isEmpty() {
		return mapping.isEmpty();
	}

	/**
	 * Returns {@code true} if this map contains a mapping for the
	 * specified key.
	 *
	 * @param key The key whose presence in this map is to be tested
	 * @return {@code true} if this map contains a mapping for the specified
	 * key.
	 */
	public boolean containsKey(Emoji key) {
		cursor.setId(key);
		return mapping.containsKey(cursor);
	}

	/**
	 * Returns {@code true} if this map maps one or more keys to the
	 * specified value.
	 *
	 * @param value value whose presence in this map is to be tested
	 * @return {@code true} if this map maps one or more keys to the
	 * specified value
	 */
	public boolean containsValue(V value) {
		return mapping.containsValue(value);
	}

	/**
	 * Returns the value to which the specified key is mapped,
	 * or {@code null} if this map contains no mapping for the key.
	 *
	 * <p>More formally, if this map contains a mapping from a key
	 * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
	 * key.equals(k))}, then this method returns {@code v}; otherwise
	 * it returns {@code null}.  (There can be at most one such mapping.)
	 *
	 * <p>A return value of {@code null} does not <i>necessarily</i>
	 * indicate that the map contains no mapping for the key; it's also
	 * possible that the map explicitly maps the key to {@code null}.
	 * The {@link #containsKey containsKey} operation may be used to
	 * distinguish these two cases.
	 *
	 * @param key The key representing the value to be retrieved
	 * @return The value mapped by given key
	 * @see #put(Emoji, Object)
	 */
	public V get(Emoji key) {
		cursor.setId(key);
		return mapping.get(cursor);
	}

	/**
	 * Associates the specified value with the specified key in this map.
	 * If the map previously contained a mapping for the key, the old
	 * value is replaced.
	 *
	 * @param key   key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 * @return the previous value associated with {@code key}, or
	 * {@code null} if there was no mapping for {@code key}.
	 * (A {@code null} return can also indicate that the map
	 * previously associated {@code null} with {@code key}.)
	 */
	@Nullable
	public V put(Emoji key, V value) {
		return mapping.put(new EmojiId(key), value);
	}

	/**
	 * Removes the mapping for the specified key from this map if present.
	 *
	 * @param key key whose mapping is to be removed from the map
	 * @return the previous value associated with {@code key}, or
	 * {@code null} if there was no mapping for {@code key}.
	 * (A {@code null} return can also indicate that the map
	 * previously associated {@code null} with {@code key}.)
	 */
	public V remove(Emoji key) {
		cursor.setId(key);
		return mapping.remove(cursor);
	}

	/**
	 * Copies all of the mappings from the specified map to this map.
	 * These mappings will replace any mappings that this map had for
	 * any of the keys currently in the specified map.
	 *
	 * @param m mappings to be stored in this map
	 * @throws NullPointerException if the specified map is null
	 */
	public void putAll(@NotNull Map<? extends Emoji, ? extends V> m) {
		m.forEach(this::put);
	}

	/**
	 * Removes all of the mappings from this map.
	 * The map will be empty after this call returns.
	 */
	public void clear() {
		mapping.clear();
	}

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 * The set is backed by the map, so changes to the map are
	 * reflected in the set, and vice-versa.  If the map is modified
	 * while an iteration over the set is in progress (except through
	 * the iterator's own {@code remove} operation), the results of
	 * the iteration are undefined.  The set supports element removal,
	 * which removes the corresponding mapping from the map, via the
	 * {@code Iterator.remove}, {@code Set.remove},
	 * {@code removeAll}, {@code retainAll}, and {@code clear}
	 * operations.  It does not support the {@code add} or {@code addAll}
	 * operations.
	 *
	 * @return a set view of the keys contained in this map
	 */
	@NotNull
	public Set<ButtonId<?>> keySet() {
		return mapping.keySet();
	}

	/**
	 * Returns a {@link Collection} view of the values contained in this map.
	 * The collection is backed by the map, so changes to the map are
	 * reflected in the collection, and vice-versa.  If the map is
	 * modified while an iteration over the collection is in progress
	 * (except through the iterator's own {@code remove} operation),
	 * the results of the iteration are undefined.  The collection
	 * supports element removal, which removes the corresponding
	 * mapping from the map, via the {@code Iterator.remove},
	 * {@code Collection.remove}, {@code removeAll},
	 * {@code retainAll} and {@code clear} operations.  It does not
	 * support the {@code add} or {@code addAll} operations.
	 *
	 * @return a view of the values contained in this map
	 */
	@NotNull
	public Collection<V> values() {
		return mapping.values();
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map.
	 * The set is backed by the map, so changes to the map are
	 * reflected in the set, and vice-versa.  If the map is modified
	 * while an iteration over the set is in progress (except through
	 * the iterator's own {@code remove} operation, or through the
	 * {@code setValue} operation on a map entry returned by the
	 * iterator) the results of the iteration are undefined.  The set
	 * supports element removal, which removes the corresponding
	 * mapping from the map, via the {@code Iterator.remove},
	 * {@code Set.remove}, {@code removeAll}, {@code retainAll} and
	 * {@code clear} operations.  It does not support the
	 * {@code add} or {@code addAll} operations.
	 *
	 * @return a set view of the mappings contained in this map
	 */
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
