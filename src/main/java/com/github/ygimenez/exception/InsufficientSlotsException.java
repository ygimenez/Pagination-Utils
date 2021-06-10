package com.github.ygimenez.exception;


public class InsufficientSlotsException extends RuntimeException {
	
	public InsufficientSlotsException() {
		super("You can only have 25 buttons per message.");
	}
}
