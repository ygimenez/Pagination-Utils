package com.github.ygimenez.model;

import java.util.Map;

/**
 * Common interface for mappers used in helpers.
 * @param <V> Class to be used for values
 */
public interface Mapping<V> {
	/**
	 * Converts this instance to a generalized map, for usage in pagination handlers.
	 * @return This instance with keys converted to {@link ButtonId}
	 */
	Map<ButtonId<?>, V> toMap();
}
