package com.github.ygimenez.model;

import com.github.ygimenez.type.ButtonOp;
import com.github.ygimenez.util.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

import java.util.Objects;
import java.util.function.BiConsumer;

public class Action {
	private final String id;
	private final Button button;
	private final BiConsumer<Message, User> event;
	private final ButtonOp type;
	private boolean active;
	private int index;

	public Action(Button button, ButtonOp type, BiConsumer<Message, User> event, boolean active, int index) {
		String id = Utils.getOr(button.getId(), button.getUrl());
		assert id != null;

		this.id = id;
		this.button = button.getStyle() == ButtonStyle.LINK ? button.withUrl(id) : button.withId(id);
		this.type = type;
		this.event = event;
		this.active = active;
		this.index = index;
	}

	public Action(Button button, ButtonOp type, BiConsumer<Message, User> event, int index) {
		String id = Utils.getOr(button.getId(), button.getUrl());
		assert id != null;

		this.id = id;
		this.button = button.getStyle() == ButtonStyle.LINK ? button.withUrl(id) : button.withId(id);
		this.type = type;
		this.event = event;
		this.index = index;
	}

	public Action(Button button, ButtonOp type, BiConsumer<Message, User> event, boolean active) {
		String id = Utils.getOr(button.getId(), button.getUrl());
		assert id != null;

		this.id = id;
		this.button = button.getStyle() == ButtonStyle.LINK ? button.withUrl(id) : button.withId(id);
		this.type = type;
		this.event = event;
		this.active = active;
	}

	public Action(Button button, ButtonOp type, BiConsumer<Message, User> event) {
		String id = Utils.getOr(button.getId(), button.getUrl());
		assert id != null;

		this.id = id;
		this.button = button.getStyle() == ButtonStyle.LINK ? button.withUrl(id) : button.withId(id);
		this.type = type;
		this.event = event;
	}

	public String getId() {
		return id;
	}

	public Button getButton() {
		return button;
	}

	public BiConsumer<Message, User> getEvent() {
		return event;
	}

	public ButtonOp getType() {
		return type;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Action action = (Action) o;
		return id.equals(action.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}