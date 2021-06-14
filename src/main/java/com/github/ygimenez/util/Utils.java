package com.github.ygimenez.util;

import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Utils {
	public static String hash(byte[] bytes, String encoding) {
		try {
			return Hex.encodeHexString(MessageDigest.getInstance(encoding).digest(bytes));
		} catch (NoSuchAlgorithmException e) {
			return "";
		}
	}

	public static <T> T getOr(T get, T or) {
		return get == null ? or : get;
	}

	public static <T> List<List<T>> chunkify(List<T> list, int chunkSize) {
		int overflow = list.size() % chunkSize;
		List<List<T>> chunks = new ArrayList<>();

		for (int i = 0; i < (list.size() - overflow) / chunkSize; i++) {
			chunks.add(list.subList(i * chunkSize, (i * chunkSize) + chunkSize));
		}

		chunks.add(list.subList(list.size() - overflow, list.size()));

		return chunks;
	}
}
