package com.github.ygimenez.model;

import com.github.ygimenez.method.Pages;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class ButtonWrapper {
	private final Member member;
	private final InteractionHook hook;
	private Message message;

	public ButtonWrapper(Member member, InteractionHook hook, Message message) {
		this.member = member;
		this.hook = hook;
		this.message = message;
	}

	public Member getMember() {
		return member;
	}

	public InteractionHook getHook() {
		return hook;
	}

	public Message getMessage() {
		return message;
	}

	public Message reloadMessage() {
		message = Pages.reloadMessage(message);
		return message;
	}

	public TextChannel getChannel() {
		return message.getTextChannel();
	}
}
