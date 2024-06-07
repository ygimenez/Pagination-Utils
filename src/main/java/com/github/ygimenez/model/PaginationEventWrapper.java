package com.github.ygimenez.model;

import com.github.ygimenez.method.Pages;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper for library events containing necessary data for handling.
 */
public class PaginationEventWrapper {
	private final Object source;
	private final User user;
	private final MessageChannel channel;
	private final Message message;
	private final Object content;
	private final InteractionHook hook;
	private final boolean isFromGuild;

	/**
	 * Constructs a new {@link PaginationEventWrapper} instance. You probably shouldn't be creating one yourself.
	 *
	 * @param source      The source event, will be either a {@link GenericMessageReactionEvent} or a {@link ButtonInteractionEvent}.
	 * @param user        The {@link User} who triggered the event.
	 * @param channel     The {@link MessageChannel} where the event happened.
	 * @param message     The {@link Message}.
	 * @param content     The button which was pressed, will be either a {@link MessageReaction} or a {@link Button}.
	 * @param isFromGuild Whether the event happened on a {@link Guild} or not.
	 */
	public PaginationEventWrapper(Object source, User user, MessageChannel channel, Message message, Object content, boolean isFromGuild) {
		if (source instanceof ButtonInteractionEvent) {
			hook = ((ButtonInteractionEvent) source).getHook();
		} else {
			hook = null;
		}

		this.source = source;
		this.user = user;
		this.channel = channel;
		this.message = message;
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
		return message.getId();
	}

	/**
	 * Retrieves the {@link Message} from the event. Will not fetch the new state if the message is ephemeral.
	 *
	 * @return The {@link Message}.
	 */
	public Message retrieveMessage() {
		if (message.isEphemeral()) {
			return message;
		}

		try {
			return Pages.subGet(channel.retrieveMessageById(message.getId()));
		} catch (InsufficientPermissionException e) {
			return message;
		}
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

	/**
	 * Retrieves the {@link InteractionHook} linked to this event.
	 *
	 * @return The {@link InteractionHook}, or null if it isn't an interaction.
	 */
	@Nullable
	public InteractionHook getHook() {
		return hook;
	}
}
