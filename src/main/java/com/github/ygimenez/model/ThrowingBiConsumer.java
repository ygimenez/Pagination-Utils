package com.github.ygimenez.model;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface ThrowingBiConsumer<A, B> extends BiConsumer<A, B> {

	void acceptThrows(A a, B b) throws Exception;

	@Override
	default void accept(A a, B b) throws RuntimeException {
		try {
			acceptThrows(a, b);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
}
