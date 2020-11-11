package com.github.ygimenez.exception;

public class NullPageException extends RuntimeException {
    public NullPageException() {
        super("The informed collection does not contain any Page");
    }
}
