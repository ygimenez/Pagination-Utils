package com.github.ygimenez.model;

import net.dv8tion.jda.internal.utils.JDALogger;
import org.slf4j.Logger;

import java.util.function.BiConsumer;


@FunctionalInterface
public interface ThrowingBiConsumer<A, B> extends BiConsumer<A, B> {
	void acceptThrows(A a, B b) throws RuntimeException;

	@Override
	default void accept(A a, B b) {
		try {
			acceptThrows(a, b);
		} catch (final Exception e) {
			Logger logger = JDALogger.getLog(this.getClass());
			logger.error(e.getMessage(), e);

			throw new RuntimeException(e);
		}
	}
}
