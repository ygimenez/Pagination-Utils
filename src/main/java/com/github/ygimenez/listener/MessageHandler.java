package com.github.ygimenez.listener;

import com.github.ygimenez.method.Pages;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class MessageHandler extends ListenerAdapter {
	private Map<String, Consumer<MessageReactionAddEvent>> events = new HashMap<>();

	public void addEvent(String id, Consumer<MessageReactionAddEvent> act) {
		events.put(id, act);
	}

	public void removeEvent(Message msg) {
		events.remove((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getUser().getId()) + msg.getId());
	}

	@Override
	public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent evt) {
		if (events.containsKey(evt.getGuild().getId() + evt.getMessageId()) && !Objects.requireNonNull(evt.getUser()).isBot())
			events.get(evt.getGuild().getId() + evt.getMessageId()).accept(evt);
	}

	@Override
	public void onMessageDelete(@Nonnull MessageDeleteEvent evt) {
		events.remove((evt.getChannelType().isGuild() ? evt.getGuild().getId() : evt.getPrivateChannel().getUser().getId()) + evt.getMessageId());
	}

	@Override
	public void onMessageReactionRemove(@Nonnull MessageReactionRemoveEvent evt) {
		if (events.containsKey(evt.getGuild().getId() + evt.getMessageId()))
			evt.getChannel().retrieveMessageById(evt.getMessageId()).queue(msg -> {
				if (!msg.getReactions().contains(evt.getReaction())) {
					if (evt.getReactionEmote().isEmoji())
						msg.addReaction(evt.getReactionEmote().getAsCodepoints()).queue(null, Pages::doNothing);
					else
						msg.addReaction(evt.getReactionEmote().getEmote()).queue(null, Pages::doNothing);
				}
			}, Pages::doNothing);
	}
}
