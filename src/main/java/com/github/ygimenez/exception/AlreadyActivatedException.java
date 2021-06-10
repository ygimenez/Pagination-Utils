package com.github.ygimenez.exception;


public class AlreadyActivatedException extends RuntimeException {
	
	public AlreadyActivatedException() {
		super("You already configured one event handler");
	}
}
