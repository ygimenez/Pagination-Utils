package com.github.ygimenez.model;

import com.github.ygimenez.exception.AlreadyActivatedException;
import com.github.ygimenez.exception.AlreadyAssignedException;
import com.github.ygimenez.exception.InvalidHandlerException;
import com.github.ygimenez.exception.InvalidStateException;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * {@link Paginator}'s builder, this class allows you to customize Pagination-Utils' behavior
 * as you like.<br>
 * If you want a quick setup, use {@link #createSimplePaginator(JDA)} or {@link #createSimplePaginator(ShardManager)}.
 */
public class PaginatorBuilder {
	private final Paginator paginator;

	/**
	 * {@link PaginatorBuilder}'s constructor, which is private.<br>
	 * Use {@link #createPaginator()} to get an instance of the builder.
	 *
	 * @param paginator The raw {@link Paginator} object to start building.
	 */
	private PaginatorBuilder(@NotNull Paginator paginator) {
		this.paginator = paginator;
	}

	/**
	 * Creates a new {@link PaginatorBuilder} instance and begin customization, use {@link #build()} to finish.
	 *
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public static PaginatorBuilder createPaginator() {
		return new PaginatorBuilder(new Paginator());
	}

	/**
	 * Creates a new {@link PaginatorBuilder} instance and begin customization, use {@link #build()} to finish.
	 *
	 * @param handler The {@link JDA} instance that'll be used for event processing.
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public static PaginatorBuilder createPaginator(@NotNull JDA handler) {
		return new PaginatorBuilder(new Paginator(handler));
	}

	/**
	 * Creates a new {@link PaginatorBuilder} instance and begin customization, use {@link #build()} to finish.
	 *
	 * @param handler The {@link ShardManager} instance that'll be used for event processing.
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public static PaginatorBuilder createPaginator(@NotNull ShardManager handler) {
		return new PaginatorBuilder(new Paginator(handler));
	}

	/**
	 * Creates a new {@link Paginator} instance using default settings.
	 *
	 * @param handler The {@link JDA} instance that'll be used for event processing.
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public static Paginator createSimplePaginator(@NotNull JDA handler) {
		Paginator p = new Paginator(handler);
		p.finishEmotes();

		return p;
	}

	/**
	 * Creates a new {@link Paginator} instance using default settings.
	 *
	 * @param handler The {@link ShardManager} instance that'll be used for event processing.
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public static Paginator createSimplePaginator(@NotNull ShardManager handler) {
		Paginator p = new Paginator(handler);
		p.finishEmotes();

		return p;
	}

	/**
	 * Retrieve the configured handler for this {@link PaginatorBuilder} instance.
	 *
	 * @return The handler object (will be either {@link JDA} or {@link ShardManager}).
	 */
	public Object getHandler() {
		return paginator.getHandler();
	}

	/**
	 * Set the handler used for event processing.
	 *
	 * @param handler The {@link JDA} instance that'll be used for event processing.
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public PaginatorBuilder setHandler(@NotNull JDA handler) {
		paginator.setHandler(handler);
		return this;
	}

	/**
	 * Set the handler used for event processing.
	 *
	 * @param handler The {@link ShardManager} instance that'll be used for event processing.
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public PaginatorBuilder setHandler(@NotNull ShardManager handler) {
		paginator.setHandler(handler);
		return this;
	}

	/**
	 * Retrieves whether user reactions will be removed after pressing the button or not.<br>
	 * If this is enabled, the bot will require {@link Permission#MESSAGE_MANAGE} permission
	 * for the buttons to work.
	 *
	 * @return Whether reactions will be removed on press or not.
	 */
	public boolean willRemoveOnReact() {
		return paginator.isRemoveOnReact();
	}

	/**
	 * Set whether user reactions will be removed after pressing the button or not.
	 * If this is enabled, the bot will require {@link Permission#MESSAGE_MANAGE} permission
	 * for the buttons to work.
	 *
	 * @param shouldRemove Whether reactions will be removed on press or not (default: false).
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public PaginatorBuilder shouldRemoveOnReact(boolean shouldRemove) {
		paginator.setRemoveOnReact(shouldRemove);
		return this;
	}

	/**
	 * Retrieves whether events will be locked to prevent double-activation.
	 *
	 * @return Whether events will be hash-locked.
	 */
	public boolean isEventLocking() {
		return paginator.isRemoveOnReact();
	}

	/**
	 * Set whether evens should be locked to prevent double-activation of buttons before
	 * it finished previous processing (can help if experiencing race condition).
	 *
	 * @param shouldLock Whether events should be locked (default: false).
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public PaginatorBuilder shouldEventLock(boolean shouldLock) {
		paginator.setEventLocked(shouldLock);
		return this;
	}

	/**
	 * Retrieves whether the {@link Message} should be deleted or not when the button handler is removed.<br>
	 * If this is enabled, the bot will require {@link Permission#MESSAGE_MANAGE} permission
	 * for the deletion to work.
	 *
	 * @return Whether the {@link Message} will be deleted or not.
	 */
	public boolean shouldDeleteOnCancel() {
		return paginator.isDeleteOnCancel();
	}

	/**
	 * Set whether {@link Message} should be deleted or not when the button handler is removed.
	 *
	 * @param deleteOnCancel Whether the {@link Message} will be deleted or not (default: false).
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public PaginatorBuilder setDeleteOnCancel(boolean deleteOnCancel) {
		paginator.setDeleteOnCancel(deleteOnCancel);
		return this;
	}

	/**
	 * Retrieve the action performed when encountering an unmapped event.
	 *
	 * @return The action to be performed.
	 */
	public ThrowingConsumer<InteractionHook> getOnRemove() {
		return paginator.getOnRemove();
	}

	/**
	 * Set the action to be performed when encountering an unmapped event. This defaults to simply removing the
	 * message buttons.
	 *
	 * @param onRemove The action to be performed (the interaction is automatically acknowledged).
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public PaginatorBuilder setOnRemove(ThrowingConsumer<InteractionHook> onRemove) {
		paginator.setOnRemove(onRemove);
		return this;
	}

	/**
	 * Retrieves an {@link Emote}'s code from the current emote {@link Map}.
	 *
	 * @param emote The {@link Emote} to be retrieved.
	 * @return The {@link Emote}'s code.
	 */
	public Emoji getEmote(@NotNull Emote emote) {
		return paginator.getEmoji(emote);
	}

	/**
	 * Modify an {@link Emote}'s code from the {@link Map}. Beware, the code must be either unicode or
	 * {@link CustomEmoji}'s mention,
	 * else the buttons <strong>WILL NOT BE ADDED</strong> and will lead to errors.
	 *
	 * @param emote The {@link Emote} to be set.
	 * @param code  The new {@link Emote}'s code.
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 * @throws InvalidHandlerException If the configured handler is not a {@link JDA} or {@link ShardManager}
	 * object.
	 */
	public PaginatorBuilder setEmote(@NotNull Emote emote, @NotNull String code) throws InvalidHandlerException {
		return setEmote(emote, Emoji.fromFormatted(code));
	}

	/**
	 * Modify an {@link Emote} from the {@link Map}.
	 *
	 * @param emote The {@link Emote} to be set.
	 * @param emoji  The new {@link Emote}'s {@link Emoji}.
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 * @throws InvalidHandlerException If the configured handler is not a {@link JDA} or {@link ShardManager}
	 * object.
	 */
	public PaginatorBuilder setEmote(@NotNull Emote emote, @NotNull Emoji emoji) throws InvalidHandlerException {
		if (paginator.getHandler() == null) throw new InvalidHandlerException();
		else if (paginator.getEmotes().containsValue(emoji)) throw new AlreadyAssignedException();

		paginator.getEmotes().put(emote, emoji);
		return this;
	}

	/**
	 * Finishes building the {@link Paginator} instance, locking further modifications.
	 *
	 * @return The {@link Paginator} instance.
	 */
	public Paginator build() {
		if (paginator.getHandler() == null) {
			throw new InvalidStateException();
		}

		paginator.finishEmotes();
		return paginator;
	}

	/**
	 * Utility terminal operation that builds the {@link Paginator} and activates it.
	 *
	 * @throws AlreadyActivatedException Thrown if there's a handler already set.
	 * @throws InvalidHandlerException Thrown if the handler isn't either a {@link JDA} or {@link ShardManager} object.
	 */
	public void activate() throws InvalidHandlerException {
		if (paginator.getHandler() == null) {
			throw new InvalidStateException();
		}

		paginator.finishEmotes();
		Pages.activate(paginator);
	}
}
