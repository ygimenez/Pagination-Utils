package com.github.ygimenez.model;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;

/**
 * Wrapper for library events containing necessary data for handling.
 */
public class PaginationEventWrapper {
	private final Object source;
	private final User user;
	private final MessageChannelUnion channel;
	private final String messageId;
	private final Object content;
	private final boolean isFromGuild;

	/**
	 * Constructs a new {@link PaginationEventWrapper} instance. You probably shouldn't be creating one yourself.
	 *
	 * @param source      The source event, will be either a {@link GenericMessageReactionEvent} or a {@link ButtonInteractionEvent}.
	 * @param user        The {@link User} who triggered the event.
	 * @param channel     The {@link TextChannel} where the event happened.
	 * @param messageId   The {@link Message} ID.
	 * @param content     The button which was pressed, will be either a {@link MessageReaction} or a {@link Button}.
	 * @param isFromGuild Whether the event happened on a {@link Guild} or not.
	 */
	public PaginationEventWrapper(Object source, User user, MessageChannelUnion channel, String messageId, Object content, boolean isFromGuild) {
		this.source = source;
		this.user = user;
		this.channel = channel;
		this.messageId = messageId;
		this.content = content;
		this.isFromGuild = isFromGuild;
	}

	/**
	 * Retrieves source event.
	 *
	 * @return The source event.
	 */
	public Object getSource() {
		return source;
	}

	/**
	 * Retrieves the {@link User} who pressed the button.
	 *
	 * @return The {@link User} who pressed the button.
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Retrieves the {@link Message} ID.
	 *
	 * @return The {@link Message} ID.
	 */
	public String getMessageId() {
		return messageId;
	}

	/**
	 * Fetch the {@link Message} from the event's {@link Message} ID.
	 *
	 * @return The {@link RestAction} for retrieving the {@link Message}.
	 */
	public RestAction<Message> retrieveMessage() {
		return channel.retrieveMessageById(messageId);
	}

	/**
	 * Retrieves the button which triggered the event.
	 *
	 * @return The button which triggered the event.
	 */
	public Object getContent() {
		return content;
	}

	/**
	 * Retrieves whether the event happened in a guild or not.
	 *
	 * @return Whether the event happened in a guild or not.
	 */
	public boolean isFromGuild() {
		return isFromGuild;
	}
}
