package com.github.ygimenez.exception;

/**
 * Exception thrown when the library has already been activated.
 */
public class AlreadyActivatedException extends RuntimeException {
	/**
	 * Default constructor.
	 */
	public AlreadyActivatedException() {
		super("You already configured one event handler");
	}
}
