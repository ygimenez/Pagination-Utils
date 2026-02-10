package com.github.ygimenez.model;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

/**
 * Represents data related to an interaction event, such as a reaction or other user-generated event.
 * This class encapsulates details about the interaction, including its unique identifier,
 * the associated message, and the user who triggered the interaction.
 */
public class InteractionData {
	private final String id;
	private final Message message;
	private final User user;

	/**
	 * Constructs a new {@code InteractionData} instance that represents an interaction event.
	 *
	 * @param id The unique identifier for the interaction. For reactions, this is the formatted reaction emoji.
	 * @param message The {@link Message} object associated with the interaction.
	 * @param user The {@link User} object representing the user who initiated the interaction.
	 */
	public InteractionData(String id, Message message, User user) {
		this.id = id;
		this.message = message;
		this.user = user;
	}

	/**
	 * Retrieves the ID for the interaction that triggered this event. For reactions, this is the formatted reaction emoji.
	 * @return The ID of the interaction.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Retrieves the {@link Message} object associated with this interaction.
	 *
	 * @return The {@link Message} related to this interaction.
	 */
	public Message getMessage() {
		return message;
	}

	/**
	 * Retrieves the {@link User} object associated with this interaction.
	 *
	 * @return The {@link User} related to this interaction.
	 */
	public User getUser() {
		return user;
	}
}
