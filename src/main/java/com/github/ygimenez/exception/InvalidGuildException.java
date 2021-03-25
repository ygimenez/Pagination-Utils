package com.github.ygimenez.exception;

/**
 * Exception thrown when trying to set an invalid lookup guild.
 */
public class InvalidGuildException extends RuntimeException {
	/**
	 * Default constructor.
	 */
	public InvalidGuildException() {
		super("One of the given guild IDs are either invalid or from a guild your bot is not member of.");
	}
}
