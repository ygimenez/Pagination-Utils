package com.github.ygimenez.exception;


public class InvalidGuildException extends RuntimeException {
	
	public InvalidGuildException() {
		super("One of the given guild IDs are either invalid or from a guild your bot is not member of.");
	}
}
