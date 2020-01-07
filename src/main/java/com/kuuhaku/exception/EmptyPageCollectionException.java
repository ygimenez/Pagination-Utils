package com.kuuhaku.exception;

public class EmptyPageCollectionException extends RuntimeException {
    public EmptyPageCollectionException() {
        super("The informed collection does not contain any Page");
    }
}
