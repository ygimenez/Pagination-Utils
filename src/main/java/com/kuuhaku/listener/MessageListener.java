package com.kuuhaku.listener;

import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

public abstract class MessageListener extends ListenerAdapter {
	@Override
	public abstract void onGenericMessageReaction(@Nonnull GenericMessageReactionEvent event);

	@Override
	public abstract void onMessageDelete(@Nonnull MessageDeleteEvent event);
}
