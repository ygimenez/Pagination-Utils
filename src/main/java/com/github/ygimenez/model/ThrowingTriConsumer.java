package com.github.ygimenez.model;

import com.github.ygimenez.method.Pages;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.BiConsumer;

/**
 * Represents an operation that accepts three input arguments and returns no
 * result.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #accept(Object, Pair)}.
 *
 * @param <A> the type of the first argument to the operation
 * @param <B> the type of the second argument to the operation
 * @param <C> the type of the third argument to the operation
 */
@FunctionalInterface
public interface ThrowingTriConsumer<A, B, C> extends BiConsumer<A, Pair<B, C>> {

	/**
	 * Performs this operation on the given arguments.
	 *
	 * @param a the first input argument
	 * @param b the second input argument
	 * @param c the third input argument
	 * @throws RuntimeException Thrown if any exception happens during lambda execution.
	 */
	void acceptThrows(A a, B b, C c) throws RuntimeException;

	@Override
	default void accept(A a, Pair<B, C> p) {
		try {
			acceptThrows(a, p.getLeft(), p.getRight());
		} catch (final Exception e) {
			Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_1, "An error occurred during consumer execution.", e);

			throw new RuntimeException(e);
		}
	}
}
