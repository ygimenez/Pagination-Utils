package com.github.ygimenez.exception;

import net.dv8tion.jda.api.entities.Emote;

/**
 * Exception thrown when trying to set an invalid emoji or {@link Emote}.
 */
public class InvalidEmoteException extends RuntimeException {
	/**
	 * Default constructor.
	 */
	public InvalidEmoteException() {
		super("One of the given emote IDs are either invalid or from a guild your bot is not member of.");
	}
}
