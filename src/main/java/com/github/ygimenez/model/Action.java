package com.github.ygimenez.model;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.Button;

import java.util.function.Consumer;

public class Action {
	private final Button button;
	private final Consumer<Message> event;

	public Action(Button button, Consumer<Message> event) {
		this.button = button;
		this.event = event;
	}

	public Button getButton() {
		return button;
	}

	public Consumer<Message> getEvent() {
		return event;
	}
}