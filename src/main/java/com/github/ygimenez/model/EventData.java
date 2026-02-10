package com.github.ygimenez.model;

import com.github.ygimenez.model.helper.BaseHelper;
import net.dv8tion.jda.api.entities.User;

/**
 * Represents an event associated with a helper of type {@link BaseHelper} and an action
 * defined by a {@link ThrowingBiConsumer}. This class is meant to encapsulate the helper and action
 * components of the event, providing methods to retrieve them for use.
 *
 * @param <Type> The type of the helper, extending {@link BaseHelper}.
 * @param <T>    The type of the content within the helper.
 */
public class EventData<Type extends BaseHelper<Type, T>, T> {
	private final BaseHelper<Type, T> helper;
	private final ThrowingBiConsumer<User, PaginationEventWrapper> action;

	/**
	 * Constructs a new {@code EventData} instance with the specified helper and action.
	 *
	 * @param helper The {@link BaseHelper} instance associated with this event.
	 *               This helper provides context and functionality that the event relies on.
	 * @param action A {@link ThrowingBiConsumer} representing the action to be performed.
	 *               This action takes a {@link User} and a {@link PaginationEventWrapper} as input arguments
	 *               and may throw an exception during its execution.
	 */
	public EventData(BaseHelper<Type, T> helper, ThrowingBiConsumer<User, PaginationEventWrapper> action) {
		this.helper = helper;
		this.action = action;
	}

	/**
	 * Retrieves the helper instance associated with this event.
	 *
	 * @return The helper instance.
	 */
	public BaseHelper<Type, T> getHelper() {
		return helper;
	}


	/**
	 * Retrieves the action associated with the event, represented as a {@link ThrowingBiConsumer}.
	 * This action performs a potentially exception-throwing operation with
	 * a {@link User} and a {@link PaginationEventWrapper} as input arguments.
	 *
	 * @return The {@link ThrowingBiConsumer} that encapsulates the action to be executed.
	 */
	public ThrowingBiConsumer<User, PaginationEventWrapper> getAction() {
		return action;
	}
}
