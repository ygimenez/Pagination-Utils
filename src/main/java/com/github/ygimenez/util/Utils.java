package com.github.ygimenez.util;

import java.util.ArrayList;
import java.util.List;

public class Utils {
	public static <T> T getOr(T get, T or) {
		return get == null ? or : get;
	}

	public static <T> List<List<T>> chunkify(List<T> list, int chunkSize) {
		int overflow = list.size() % chunkSize;
		List<List<T>> chunks = new ArrayList<>();

		for (int i = 0; i < (list.size() - overflow) / chunkSize; i++) {
			chunks.add(list.subList(i * chunkSize, (i * chunkSize) + chunkSize));
		}

		if (overflow != 0)
			chunks.add(list.subList(list.size() - overflow, list.size()));

		return chunks;
	}
}
