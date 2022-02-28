package com.github.ygimenez.exception;

/**
 * Exception thrown when trying to add an emote already assigned.
 */
public class AlreadyAssignedException extends RuntimeException {
	/**
	 * Default constructor.
	 */
	public AlreadyAssignedException() {
		super("Emote was already assigned to another button");
	}
}
