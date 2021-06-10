package com.github.ygimenez.model;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;


public class Page {
	private final Object content;
	private final List<Action> buttons;
	private final int group;
	
	public Page(@Nonnull Object content) throws IllegalArgumentException {
		if (!(content instanceof Message) && !(content instanceof MessageEmbed))
			throw new IllegalArgumentException("Page content must be either a Message or a MessageEmbed");

		this.content = content;
		this.buttons = new ArrayList<>();
		this.group = -1;
	}
	
	public Page(@Nonnull Object content, @Nonnull List<Action> buttons, int group) throws IllegalArgumentException {
		if (!(content instanceof Message) && !(content instanceof MessageEmbed))
			throw new IllegalArgumentException("Page content must be either a Message or a MessageEmbed");

		this.content = content;
		this.buttons = buttons;
		this.group = group;
	}
	
	public Page(@Nonnull Object content, @Nonnull List<Action> buttons) throws IllegalArgumentException {
		if (!(content instanceof Message) && !(content instanceof MessageEmbed))
			throw new IllegalArgumentException("Page content must be either a Message or a MessageEmbed");

		this.content = content;
		this.buttons = buttons;
		this.group = -1;
	}
	
	public Page(@Nonnull Object content, int group) throws IllegalArgumentException {
		if (!(content instanceof Message) && !(content instanceof MessageEmbed))
			throw new IllegalArgumentException("Page content must be either a Message or a MessageEmbed");

		this.content = content;
		this.buttons = new ArrayList<>();
		this.group = group;
	}
	
	public Object getContent() {
		return content;
	}

	public List<Action> getButtons() {
		return buttons;
	}
	
	public int getGroup() {
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
