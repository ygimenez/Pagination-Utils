package com.github.ygimenez.model;

import com.github.ygimenez.method.Pages;

import java.util.function.Consumer;

/**
 * Represents an operation that accepts one input argument and returns no
 * result. This is the throwing specialization of {@link Consumer}.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #accept(Object)}.
 *
 * @param <A> the type of the first argument to the operation
 */
@FunctionalInterface
public interface ThrowingConsumer<A> extends Consumer<A> {

	/**
	 * Performs this operation on the given arguments.
	 *
	 * @param a the first input argument
	 * @throws RuntimeException Thrown if any exception happens during lambda execution.
	 */
	void acceptThrows(A a) throws RuntimeException;

	@Override
	default void accept(A a) {
		try {
			acceptThrows(a);
		} catch (final Exception e) {
			Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_1, "An error occurred during consumer execution.", e);

			throw new RuntimeException(e);
		}
	}
}
