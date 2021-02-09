package com.github.ygimenez.exception;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;

/**
 * Exception thrown when trying to set an invalid handler (not {@link JDA} or {@link ShardManager}).
 */
public class InvalidHandlerException extends Exception {
	/**
	 * Default constructor.
	 */
	public InvalidHandlerException() {
		super("Handler must be either a JDA or ShardManager object.");
	}
}
