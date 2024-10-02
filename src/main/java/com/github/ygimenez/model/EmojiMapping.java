package com.github.ygimenez.model;

import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Effectively a {@link HashMap} with {@link Emoji} key and {@link Page} values, with built-in conversion.
 */
public class EmojiMapping<V> extends HashMap<Emoji, V> implements Mapping<V> {
	public Map<ButtonId<?>, V> toMap() {
		return entrySet().stream()
				.map(e -> Map.entry(new EmojiId(e.getKey()), e.getValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}
