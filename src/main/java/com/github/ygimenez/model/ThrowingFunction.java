package com.github.ygimenez.model;

import com.github.ygimenez.method.Pages;

import java.util.function.Function;

/**
 * Represents a function that accepts one argument and produces a result.
 * This is the throwing specialization of {@link Function}.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #apply(Object)}.
 *
 * @param <T> the type of the input to the function.
 * @param <R> the type of the result of the function.
 */
@FunctionalInterface
public interface ThrowingFunction<T, R> extends Function<T, R> {

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the function argument.
     * @return the function result.
     * @throws RuntimeException Thrown if any exception happens during lambda execution.
     */
    R applyThrows(T t) throws RuntimeException;

    @Override
    default R apply(T t) {
        try {
            return applyThrows(t);
        } catch (final Exception e) {
            Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_1, "An error occurred during function execution.", e);

            throw new RuntimeException(e);
        }
    }
}
