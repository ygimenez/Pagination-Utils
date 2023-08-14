package com.github.ygimenez.model;

import com.github.ygimenez.method.Pages;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Wrapper for {@link Pages#buttonize} arguments containing necessary data for processing.
 */
public class ButtonWrapper {
	private final User user;
	private final InteractionHook hook;
	private final Button button;
	private final Map<String, List<?>> dropdownValues;
	private Message message;

	/**
	 * Constructs a new {@link ButtonWrapper} instance. You probably shouldn't be creating one yourself.
	 *
	 * @param user           The {@link User} who pressed the button.
	 * @param hook           The {@link InteractionHook} referencing interaction event, or null if it's not an interaction.
	 * @param button         The {@link Button} that triggered this event.
	 * @param dropdownValues The currently selected {@link SelectMenu} values, if any.
	 * @param message        The parent {@link Message}.
	 */
	public ButtonWrapper(User user, InteractionHook hook, Button button, Map<String, List<?>> dropdownValues, Message message) {
		this.user = user;
		this.hook = hook;
		this.button = button;
		this.dropdownValues = dropdownValues;
		this.message = message;
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
	 * Retrieves the {@link Member} who pressed the button.
	 *
	 * @return The {@link Member} who pressed the button.
	 * @throws IllegalStateException If the event didn't happen in a {@link Guild}.
	 */
	public Member getMember() throws IllegalStateException {
		return Pages.subGet(message.getGuild().retrieveMember(user));
	}

	/**
	 * Retrieves the event's {@link InteractionHook}.
	 *
	 * @return The {@link InteractionHook} referencing interaction event, or null if it's not an interaction.
	 */
	@Nullable
	public InteractionHook getHook() {
		return hook;
	}

	/**
	 * Retrieves the {@link Button} that triggered the event.
	 *
	 * @return The {@link Button} that triggered the event, or null if it's not an interaction.
	 */
	@Nullable
	public Button getButton() {
		return button;
	}

	public Map<String, List<?>> getDropdownValues() {
		return dropdownValues;
	}

	/**
	 * Retrieves the parent {@link Message}.
	 *
	 * @return The parent {@link Message}.
	 */
	public Message getMessage() {
		return message;
	}

	/**
	 * Reloads current parent {@link Message} instance, retrieving the latest state of it (useful when doing {@link Button}
	 * updates or actions which depend on message content).
	 *
	 * @return The reloaded {@link Message}.
	 */
	public Message reloadMessage() {
		message = Pages.reloadMessage(message);
		return message;
	}

	/**
	 * Shortcut for retrieving the {@link Message}'s {@link MessageChannel}.
	 *
	 * @return The parent {@link Message}'s {@link MessageChannel}.
	 */
	public MessageChannel getChannel() {
		return message.getChannel();
	}
}
