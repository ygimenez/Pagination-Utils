package com.github.ygimenez.model;

import com.coder4.emoji.EmojiUtils;
import com.github.ygimenez.exception.*;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
	 *                (must be either {@link JDA} or {@link ShardManager}).
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public static Paginator createSimplePaginator(@Nonnull Object handler) {
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
	 * Sets the handler used for event processing.
	 *
	 * @param handler The handler that'll be used for event processing
	 *                (must be either {@link JDA} or {@link ShardManager}).
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 * @throws InvalidHandlerException If the supplied handler is not a {@link JDA} or {@link ShardManager}
	 * object.
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
	 * Sets whether evens should be locked to prevent double-activation of buttons before
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
	 * Sets whether {@link Message} should be deleted or not when the button handler is removed.
	 * <strong>This must only be called by {@link PaginatorBuilder}</strong>.
	 *
	 * @param deleteOnCancel Whether the {@link Message} will be deleted or not (default: false).
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 */
	public PaginatorBuilder setDeleteOnCancel(boolean deleteOnCancel) {
		paginator.setDeleteOnCancel(deleteOnCancel);
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
	 * Modify an {@link Emote}'s code from the {@link Map}. Beware, the code must be either unicode or
	 * {@link net.dv8tion.jda.api.entities.Emote}'s ID,
	 * else the buttons <strong>WILL NOT BE ADDED</strong> and will lead to errors.
	 *
	 * @param emote The {@link Emote} to be set.
	 * @param code  The new {@link Emote}'s code.
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 * @throws InvalidHandlerException If the configured handler is not a {@link JDA} or {@link ShardManager}
	 * object.
	 * @throws InvalidEmoteException If the supplied {@link net.dv8tion.jda.api.entities.Emote} is not from
	 * a guild your bot's in and is not unicode.
	 */
	public PaginatorBuilder setEmote(@Nonnull Emote emote, @Nonnull String code) throws InvalidHandlerException {
		if (StringUtils.isNumeric(code)) {
			net.dv8tion.jda.api.entities.Emote e = null;
			if (paginator.getHandler() instanceof JDA) {
				JDA handler = (JDA) paginator.getHandler();

				if (handler.getEmotes().isEmpty()) {
					Guild g = handler.getGuildById(paginator.getEmoteCache().getOrDefault(code, "0"));

					if (g != null) {
						e = g.retrieveEmoteById(code).complete();
					} else for (Guild guild : handler.getGuilds()) {
						try {
							e = guild.retrieveEmoteById(code).complete();
							break;
						} catch (ErrorResponseException ignore) {
						}
					}

					if (e != null && e.getGuild() != null)
						paginator.getEmoteCache().put(code, e.getGuild().getId());
				} else e = handler.getEmoteById(code);
			} else if (paginator.getHandler() instanceof ShardManager) {
				ShardManager handler = (ShardManager) paginator.getHandler();

				if (handler.getEmotes().isEmpty()) {
					Guild g = handler.getGuildById(paginator.getEmoteCache().getOrDefault(code, "0"));

					if (g != null) {
						e = g.retrieveEmoteById(code).complete();
					} else for (Guild guild : handler.getGuilds()) {
						try {
							e = guild.retrieveEmoteById(code).complete();
							break;
						} catch (ErrorResponseException ignore) {
						}
					}

					if (e != null && e.getGuild() != null)
						paginator.getEmoteCache().put(code, e.getGuild().getId());
				} else e = handler.getEmoteById(code);
			} else throw new InvalidHandlerException();

			if (e == null) throw new InvalidEmoteException();
			paginator.getEmotes().put(emote, e.getName() + ":" + e.getId());
		} else if (EmojiUtils.containsEmoji(code)) {
			paginator.getEmotes().put(emote, code);
		} else throw new InvalidEmoteException();
		return this;
	}

	/**
	 * Defines the guild IDs to be used for {@link net.dv8tion.jda.api.entities.Emote} lookup. This has no
	 * effect when {@link CacheFlag#EMOTE} is enabled.
	 *
	 * @param lookupGuilds The {@link List} containing guild IDs to be used for {@link net.dv8tion.jda.api.entities.Emote} lookup.
	 * @return The {@link PaginatorBuilder} instance for chaining convenience.
	 * @throws InvalidHandlerException If the configured handler is not a {@link JDA} or {@link ShardManager}
	 * object.
	 * @throws InvalidGuildException If one of the supplied guild IDs is invalid or from a guild your bot is not member of.
	 */
	public PaginatorBuilder setLookupGuilds(List<String> lookupGuilds) throws InvalidHandlerException {
		if (paginator.getHandler() instanceof JDA) {
			JDA handler = (JDA) paginator.getHandler();

			List<String> guilds = handler.getGuilds().stream()
					.map(Guild::getId)
					.collect(Collectors.toList());

			if (!guilds.containsAll(lookupGuilds)) throw new InvalidGuildException();
		} else if (paginator.getHandler() instanceof ShardManager) {
			ShardManager handler = (ShardManager) paginator.getHandler();

			List<String> guilds = handler.getGuilds().stream()
					.map(Guild::getId)
					.collect(Collectors.toList());

			if (!guilds.containsAll(lookupGuilds)) throw new InvalidGuildException();
		} else throw new InvalidHandlerException();

		paginator.setLookupGuilds(lookupGuilds);
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

		paginator.finishEmotes();
		paginator.finishLookupGuilds();
		return paginator;
	}

	/**
	 * Utility terminal operation that builds the {@link Paginator} and activates it.
	 *
	 * @throws AlreadyActivatedException Thrown if there's a handler already set.
	 * @throws InvalidHandlerException Thrown if the handler isn't either a {@link JDA} or {@link ShardManager} object.
	 */
	public void activate() throws InvalidHandlerException {
		if (paginator.getHandler() == null)
			throw new InvalidStateException();

		paginator.finishEmotes();
		paginator.finishLookupGuilds();
		Pages.activate(paginator);
	}
}
