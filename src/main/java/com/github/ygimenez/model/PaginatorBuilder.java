package com.github.ygimenez.model;

import com.github.ygimenez.exception.InvalidHandlerException;
import com.github.ygimenez.exception.InvalidStateException;
import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * {@link Paginator}'s builder, this class allows you to customize Pagination-Utils' behavior
 * as you like. <br>
 * If you want a quick setup, use {@link #createSimplePaginator(Object)}.
 */
public class PaginatorBuilder {
	private final Paginator paginator;

	/**
	 * {@link PaginatorBuilder}'s constructor, which is private.<br>
	 * Use {@link #createPaginator()} to get an instance of the builder.
	 *
	 * @param paginator The raw {@link Paginator} object to start building.
	 */
	private PaginatorBuilder(@Nonnull Paginator paginator) {
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
	 * Creates a new {@link Paginator} instance using default settings.
	 *
	 * @param handler The handler that'll be used for event processing
	 * (must be either {@link JDA} or {@link ShardManager}).
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public static Paginator createSimplePaginator(@Nonnull Object handler) {
		Paginator p = new Paginator(handler);
		p.setEmotes();

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
	 * Sets the handler used for event processing.
	 *
	 * @param handler The handler that'll be used for event processing
	 * (must be either {@link JDA} or {@link ShardManager}).
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public PaginatorBuilder setHandler(@Nonnull Object handler) throws InvalidHandlerException {
		if (!(handler instanceof JDA) && !(handler instanceof ShardManager))
			throw new InvalidHandlerException();

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
	 * Sets whether user reactions will be removed after pressing the button or not.
	 * If this is enabled, the bot will require {@link Permission#MESSAGE_MANAGE} permission
	 * for the buttons to work.
	 *
	 * @param shouldRemove Whether reactions will be removed on press or not.
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public PaginatorBuilder shouldRemoveOnReact(boolean shouldRemove) {
		paginator.setRemoveOnReact(shouldRemove);
		return this;
	}

	/**
	 * Retrieves an {@link Emote}'s code from the current emote {@link Map}.
	 *
	 * @param emote The {@link Emote} to be retrieved.
	 * @return The {@link Emote}'s code.
	 */
	public String getEmote(@Nonnull Emote emote) {
		return paginator.getEmotes().get(emote);
	}

	/**
	 * Modify an {@link Emote}'s code from the {@link Map}. Beware, the code must be in either unicode or Discord
	 * notation, else the buttons <strong>WILL NOT BE ADDED</strong> and will lead to errors.
	 *
	 * @param emote The {@link Emote} to be set.
	 * @param code The new {@link Emote}'s code.
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public PaginatorBuilder setEmote(@Nonnull Emote emote, @Nonnull String code) {
		paginator.getEmotes().put(emote, code);
		return this;
	}

	/**
	 * Finishes building the {@link Paginator} instance, locking further modifications.
	 *
	 * @return The {@link Paginator} instance.
	 */
	public Paginator build() {
		if (paginator.getHandler() == null)
			throw new InvalidStateException();

		paginator.setEmotes();
		return paginator;
	}
}
