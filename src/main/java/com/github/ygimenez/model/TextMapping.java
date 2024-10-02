package com.github.ygimenez.model;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Effectively a {@link HashMap} with {@link String} key and {@link Page} values, with built-in conversion.
 */
public class TextMapping<V> extends HashMap<String, V> implements Mapping<V> {
	public Map<ButtonId<?>, V> toMap() {
		return entrySet().stream()
				.map(e -> Map.entry(new TextId(e.getKey()), e.getValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}
