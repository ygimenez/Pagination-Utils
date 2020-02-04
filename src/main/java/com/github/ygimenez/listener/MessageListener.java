package com.github.ygimenez.listener;

import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

public abstract class MessageListener extends ListenerAdapter {
	@Override
	public abstract void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event);

	@Override
	public abstract void onMessageDelete(@Nonnull MessageDeleteEvent event);
}
