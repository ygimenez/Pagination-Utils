package com.github.ygimenez.exception;

import net.dv8tion.jda.api.entities.Emote;


public class InvalidEmoteException extends RuntimeException {
	
	public InvalidEmoteException() {
		super("One of the given emote IDs are either invalid or from a guild your bot is not member of.");
	}
}
