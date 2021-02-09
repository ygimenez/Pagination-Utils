package com.github.ygimenez.exception;

/**
 * Exception thrown when no handler has been set.
 */
public class InvalidStateException extends RuntimeException {
	/**
	 * Default constructor.
	 */
	public InvalidStateException() {
		super("No active handler has been set");
	}
}
