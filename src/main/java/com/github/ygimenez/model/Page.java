package com.github.ygimenez.model;

import com.github.ygimenez.type.ButtonOp;
import com.github.ygimenez.util.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


public class Page {
	private final Object content;
	private final Set<Action> buttons;
	private final String group;
	
	public Page(@Nonnull Object content) throws IllegalArgumentException {
		if (!(content instanceof Message) && !(content instanceof MessageEmbed))
			throw new IllegalArgumentException("Page content must be either a Message or a MessageEmbed");

		this.content = content;
		this.buttons = new HashSet<>();
		this.group = null;
	}
	
	public Page(@Nonnull Object content, @Nonnull Set<Action> buttons, String group) throws IllegalArgumentException {
		if (!(content instanceof Message) && !(content instanceof MessageEmbed))
			throw new IllegalArgumentException("Page content must be either a Message or a MessageEmbed");

		this.content = content;
		this.buttons = buttons;
		this.group = group;
	}
	
	public Page(@Nonnull Object content, @Nonnull Set<Action> buttons) throws IllegalArgumentException {
		if (!(content instanceof Message) && !(content instanceof MessageEmbed))
			throw new IllegalArgumentException("Page content must be either a Message or a MessageEmbed");

		this.content = content;
		this.buttons = buttons;
		this.group = null;
	}
	
	public Page(@Nonnull Object content, String group) throws IllegalArgumentException {
		if (!(content instanceof Message) && !(content instanceof MessageEmbed))
			throw new IllegalArgumentException("Page content must be either a Message or a MessageEmbed");

		this.content = content;
		this.buttons = new HashSet<>();
		this.group = group;
	}
	
	public Object getContent() {
		return content;
	}

	public void addButton(ButtonOp type, int index, Button button, BiConsumer<Message, User> action) {
		buttons.add(new Action(button, type, action, index));
	}

	public void addButton(ButtonOp type, Button button, BiConsumer<Message, User> action) {
		buttons.add(new Action(button, type, action));
	}

	public void addButton(Button button, BiConsumer<Message, User> action) {
		buttons.add(new Action(button, ButtonOp.CUSTOM, action));
	}

	public void addButtons(Action... buttons) {
		this.buttons.addAll(List.of(buttons));
	}

	public Set<Action> getButtons() {
		return Collections.unmodifiableSet(buttons);
	}

	public List<ActionRow> getActionRows() {
		List<ActionRow> rows = new ArrayList<>();

		List<List<Button>> custom = Utils.chunkify(buttons.stream()
				.sorted(Comparator.comparingInt(Action::getIndex))
				.map(Action::getButton)
				.collect(Collectors.toList()), 5);

		for (List<Button> buttons : custom) {
			rows.add(ActionRow.of(buttons));
		}

		return rows;
	}
	
	public String getGroup() {
		return group;
	}
	
	@Override
	public String toString() {
		if (content instanceof Message) {
			return ((Message) content).getContentRaw();
		} else if (content instanceof MessageEmbed) {
			return ((MessageEmbed) content).getDescription();
		} else {
			return "Unknown type";
		}
	}
}
