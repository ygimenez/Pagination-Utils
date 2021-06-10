package com.github.ygimenez.exception;

import java.util.List;


public class NullPageException extends RuntimeException {
    
    public NullPageException() {
        super("The informed collection does not contain any Page");
    }
}
