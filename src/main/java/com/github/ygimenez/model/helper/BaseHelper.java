package com.github.ygimenez.model.helper;

import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

abstract class BaseHelper<Helper extends BaseHelper<Helper, T>, T> {
	private final Class<Helper> subClass;

	private final T content;
	private final boolean useButtons;

	private boolean cancellable = true;
	private long time = 0;
	private Predicate<User> canInteract = null;

	protected BaseHelper(Class<Helper> subClass, T buttons, boolean useButtons) {
		this.subClass = subClass;
		this.content = buttons;
		this.useButtons = useButtons;
	}

	/**
	 * Retrieves the collection used by this helper to store the pages.
	 *
	 * @return The underlying collection.
	 */
	public T getContent() {
		return content;
	}

	/**
	 * Returns whether the event is configured to use buttons or not.
	 *
	 * @return Whether the event is configured to use buttons or not.
	 */
	public boolean isUsingButtons() {
		return useButtons;
	}

	/**
	 * Returns whether the {@link Emote#CANCEL} button will be included or not.
	 *
	 * @return Whether the event is cancellable or not.
	 */
	public boolean isCancellable() {
		return cancellable;
	}

	/**
	 * Sets whether the event is cancellable through {@link Emote#CANCEL}.
	 *
	 * @param cancellable Whether the event can be cancelled or not (default: true).
	 * @return The {@link Helper} instance for chaining convenience.
	 */
	public Helper setCancellable(boolean cancellable) {
		this.cancellable = cancellable;
		return subClass.cast(this);
	}

	/**
	 * Retrieves the timeout for the event, in milliseconds.
	 *
	 * @return The timeout for the event.
	 */
	public long getTimeout() {
		return time;
	}

	/**
	 * Sets the timeout for automatically cancelling the event. Values less than or equal to zero will disable the
	 * timeout.
	 *
	 * @param time The time for the timeout.
	 * @param unit The unit for the timeout.
	 * @return The {@link Helper} instance for chaining convenience.
	 */
	public Helper setTimeout(int time, TimeUnit unit) {
		this.time = TimeUnit.MILLISECONDS.convert(time, unit);
		return subClass.cast(this);
	}

	/**
	 * Checks whether the supplied {@link User} can interact with the event.
	 *
	 * @param user The {@link User} to check.
	 * @return Whether the suppied user can interact with the event.
	 */
	public boolean canInteract(User user) {
		return canInteract == null || canInteract.test(user);
	}

	/**
	 * Sets the condition used to check if a given user can interact with the event buttons.
	 *
	 * @param canInteract A {@link Predicate} for checking if a given user can interact with the buttons (default: null).
	 * @return The {@link Helper} instance for chaining convenience.
	 */
	public Helper setCanInteract(@Nullable Predicate<User> canInteract) {
		this.canInteract = canInteract;
		return subClass.cast(this);
	}

	/**
	 * Prepares the message for being used by the library. This doesn't need to be called manually, this will
	 * be called during normal flow.
	 * <br>
	 * This is no-op when using reaction buttons.
	 * <br><br>
	 * Example:
	 * <pre>{@code helper.apply(channel.sendMessage("Hello world!")).queue();}</pre>
	 *
	 * @param action A message event (either create or edit).
	 * @return The same event, but modified to include the buttons.
	 * @param <Out> Generic for a {@link MessageRequest}
	 */
	public abstract <Out extends MessageRequest<Out>> Out apply(Out action);

	/**
	 * Calculates whether the {@link Message} needs to have buttons applied onto or not.
	 *
	 * @param msg The {@link Message} to be checked.
	 * @return Whether it needs to be updated or not.
	 */
	public abstract boolean shouldUpdate(Message msg);
}
