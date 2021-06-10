package com.github.ygimenez.exception;


public class InvalidHandlerException extends Exception {
	
	public InvalidHandlerException() {
		super("Handler must be either a JDA or ShardManager object.");
	}
}
