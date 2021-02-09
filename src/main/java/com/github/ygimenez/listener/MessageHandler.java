package com.github.ygimenez.listener;

import com.github.ygimenez.method.Pages;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Class responsible for handling reaction events sent by the handler.<br>
 * Only one event is added to the handler to prevent cluttering and unnecessary listeners.
 */
public class MessageHandler extends ListenerAdapter {
	private final Map<String, Consumer<GenericMessageReactionEvent>> events = new HashMap<>();

	/**
	 * Adds an event to the handler, which will be executed whenever a button with the same
	 * ID is pressed.
	 *
	 * @param id The ID of the event.
	 * @param act The action to be executed when the button is pressed.
	 */
	public void addEvent(String id, Consumer<GenericMessageReactionEvent> act) {
		events.put(id, act);
	}

	/**
	 * Removes an event from the handler.
	 *
	 * @param msg The {@link Message} which had attached events.
	 */
	public void removeEvent(Message msg) {
		switch (msg.getChannelType()) {
			case TEXT:
				events.remove(msg.getGuild().getId() + msg.getId());
				break;
			case PRIVATE:
				events.remove(msg.getPrivateChannel().getId() + msg.getId());
				break;
		}
	}

	@Override
	public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent evt) {
		evt.retrieveUser().submit().thenAccept(u -> {
			if (u.isBot()) return;

			switch (evt.getChannelType()) {
				case TEXT:
					if (events.containsKey(evt.getGuild().getId() + evt.getMessageId()))
						events.get(evt.getGuild().getId() + evt.getMessageId()).accept(evt);
					break;
				case PRIVATE:
					if (events.containsKey(evt.getPrivateChannel().getId() + evt.getMessageId()))
						events.get(evt.getPrivateChannel().getId() + evt.getMessageId()).accept(evt);
					break;
			}
		});
	}

	@Override
	public void onMessageDelete(@Nonnull MessageDeleteEvent evt) {
		switch (evt.getChannelType()) {
			case TEXT:
				events.remove(evt.getGuild().getId() + evt.getMessageId());
				break;
			case PRIVATE:
				events.remove(evt.getPrivateChannel().getId() + evt.getMessageId());
				break;
		}
	}

	@Override
	public void onMessageReactionRemove(@Nonnull MessageReactionRemoveEvent evt) {
		evt.retrieveUser().submit().thenAccept(u -> {
			if (u.isBot()) return;

			boolean removeOnReact =
					(Pages.isActivated() && Pages.getPaginator() == null) || (Pages.isActivated() && Pages.getPaginator().isRemoveOnReact());

			if (evt.isFromGuild() && removeOnReact) {
				evt.getPrivateChannel().retrieveMessageById(evt.getMessageId()).submit().thenAccept(msg -> {
					switch (evt.getChannelType()) {
						case TEXT:
							if (events.containsKey(evt.getGuild().getId() + msg.getId()) && !msg.getReactions().contains(evt.getReaction())) {
								if (evt.getReactionEmote().isEmoji())
									msg.addReaction(evt.getReactionEmote().getAsCodepoints()).submit();
								else
									msg.addReaction(evt.getReactionEmote().getEmote()).submit();
							}
							break;
						case PRIVATE:
							if (events.containsKey(evt.getPrivateChannel().getId() + msg.getId()) && !msg.getReactions().contains(evt.getReaction())) {
								if (evt.getReactionEmote().isEmoji())
									msg.addReaction(evt.getReactionEmote().getAsCodepoints()).submit();
								else
									msg.addReaction(evt.getReactionEmote().getEmote()).submit();
							}
							break;
					}
				});
			} else {
				switch (evt.getChannelType()) {
					case TEXT:
						if (events.containsKey(evt.getGuild().getId() + evt.getMessageId()))
							events.get(evt.getGuild().getId() + evt.getMessageId()).accept(evt);
						break;
					case PRIVATE:
						if (events.containsKey(evt.getPrivateChannel().getId() + evt.getMessageId()))
							events.get(evt.getPrivateChannel().getId() + evt.getMessageId()).accept(evt);
						break;
				}
			}
		});
	}
}
