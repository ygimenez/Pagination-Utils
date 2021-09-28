package com.github.ygimenez.model;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;

public class PaginationEventWrapper {
	private final Object source;
	private final User user;
	private final MessageChannel channel;
	private final String messageId;
	private final Object content;
	private final boolean isFromGuild;

	public PaginationEventWrapper(Object source, User user, MessageChannel channel, String messageId, Object content, boolean isFromGuild) {
		this.source = source;
		this.user = user;
		this.channel = channel;
		this.messageId = messageId;
		this.content = content;
		this.isFromGuild = isFromGuild;
	}

	public Object getSource() {
		return source;
	}

	public User getUser() {
		return user;
	}

	public Member getMember() {
		return isFromGuild ? ((TextChannel) channel).getGuild().getMember(user) : null;
	}

	public MessageChannel getChannel() {
		return channel;
	}

	public String getMessageId() {
		return messageId;
	}

	public RestAction<Message> retrieveMessage() {
		return channel.retrieveMessageById(messageId);
	}

	public Object getContent() {
		return content;
	}

	public boolean isFromGuild() {
		return isFromGuild;
	}
}
