package com.github.ygimenez.exception;

import java.util.List;

/**
 * Exception thrown when trying to paginate an empty {@link List}.
 */
public class NullPageException extends RuntimeException {
    /**
     * Default constructor.
     */
    public NullPageException() {
        super("The informed collection does not contain any Page");
    }
}
