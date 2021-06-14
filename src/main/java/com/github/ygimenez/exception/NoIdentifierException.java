package com.github.ygimenez.exception;


public class NoIdentifierException extends RuntimeException {

    public NoIdentifierException() {
        super("All custom buttons must have an ID.");
    }
}
