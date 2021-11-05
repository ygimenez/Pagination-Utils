package com.github.ygimenez.model;

import com.github.ygimenez.model.PUtilsConfig.LogLevel;
import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.slf4j.Logger;

import java.util.*;

/**
 * This is the core object for Pagination-Utils' settings.<br>
 * <br>
 * All settings changed during {@link Paginator} creation will reflect across the whole library,
 * allowing further customization of it.<br>
 * <br>
 * <strong>This class must only be instantiated by {@link PaginatorBuilder}</strong>.
 */
public class Paginator {
	private Object handler = null;
	private boolean removeOnReact = false;
	private boolean eventLocked = false;
	private boolean deleteOnCancel = false;
	private Map<Emote, Emoji> emotes = new EnumMap<>(Emote.class);
	private Logger logger = null;

	/**
	 * You shouldn't create a {@link Paginator} instance directly, please use {@link PaginatorBuilder}.
	 */
	protected Paginator() {
	}

	/**
	 * You shouldn't create a {@link Paginator} instance directly, please use {@link PaginatorBuilder}.
	 *
	 * @param handler The handler that'll be used for event processing
	 *                (must be either {@link JDA} or {@link ShardManager}).
	 */
	public Paginator(Object handler) {
		this.handler = handler;
	}

	/**
	 * Retrieves the handler used during this {@link Paginator}'s construction.
	 *
	 * @return The handler object (will be either {@link JDA} or {@link ShardManager}).
	 */
	public Object getHandler() {
		return handler;
	}

	/**
	 * Sets the handler used for event processing.
	 * <strong>This must only be called by {@link PaginatorBuilder}</strong>.
	 *
	 * @param handler The handler that'll be used for event processing
	 *                (must be either {@link JDA} or {@link ShardManager}).
	 */
	protected void setHandler(Object handler) {
		this.handler = handler;
		this.logger = JDALogger.getLog("Pagination-Utils");
	}

	/**
	 * Retrieves whether user reactions will be removed after pressing the button or not.<br>
	 * If this is enabled, the bot will require {@link Permission#MESSAGE_MANAGE} permission
	 * for the buttons to work.
	 *
	 * @return Whether reactions will be removed on press or not.
	 */
	public boolean isRemoveOnReact() {
		return removeOnReact;
	}

	/**
	 * Sets whether user reactions will be removed after pressing the button or not.
	 * <strong>This must only be called by {@link PaginatorBuilder}</strong>.
	 *
	 * @param removeOnReact Whether reactions will be removed on press or not.
	 */
	protected void setRemoveOnReact(boolean removeOnReact) {
		this.removeOnReact = removeOnReact;
	}

	/**
	 * Retrieves whether events will be locked to prevent double-activation
	 * of buttons before it finished previous processing (can help if experiencing race condition).
	 *
	 * @return Whether events will be hash-locked.
	 */
	public boolean isEventLocked() {
		return eventLocked;
	}

	/**
	 * Sets whether evens should be locked to prevent double-activation.
	 * <strong>This must only be called by {@link PaginatorBuilder}</strong>.
	 *
	 * @param hashLocking Whether events should be locked.
	 */
	protected void setEventLocked(boolean hashLocking) {
		this.eventLocked = hashLocking;
	}

	/**
	 * Retrieves whether the {@link Message} should be deleted or not when the button handler is removed.<br>
	 * If this is enabled, the bot will require {@link Permission#MESSAGE_MANAGE} permission
	 * for the deletion to work.
	 *
	 * @return Whether the {@link Message} will be deleted or not.
	 */
	public boolean isDeleteOnCancel() {
		return deleteOnCancel;
	}

	/**
	 * Sets whether {@link Message} should be deleted or not when the button handler is removed.
	 * <strong>This must only be called by {@link PaginatorBuilder}</strong>.
	 *
	 * @param deleteOnCancel Whether the {@link Message} will be deleted or not.
	 */
	protected void setDeleteOnCancel(boolean deleteOnCancel) {
		this.deleteOnCancel = deleteOnCancel;
	}

	/**
	 * The {@link Map} containing configured {@link Emote}s for this {@link Paginator}. This {@link Map}
	 * must not be modified after construction of this {@link Paginator}.
	 *
	 * @return The {@link Map} containing configured {@link Emote}s for this {@link Paginator}.
	 */
	public Map<Emote, Emoji> getEmotes() {
		return emotes;
	}

	/**
	 * Same as {@link #getEmotes()} but this method will turn {@link net.dv8tion.jda.api.entities.Emote} mentions
	 * into IDs.
	 *
	 * @param emote The {@link Emote} to be defined.
	 * @return The {@link Emoji} representing this {@link Emote}.
	 */
	public Emoji getEmote(Emote emote) {
		return emotes.getOrDefault(emote, emote.getDefault());
	}

	/**
	 * Same as {@link #getEmotes()} but this method will turn {@link net.dv8tion.jda.api.entities.Emote} mentions
	 * into IDs.
	 *
	 * @param emote The {@link Emote} to be defined.
	 * @return The {@link Emoji} representing this {@link Emote}.
	 */
	public String getStringEmote(Emote emote) {
		return getEmote(emote).getAsMention().replaceAll("[<>]", "");
	}

	/**
	 * Make configured {@link Emote}s final.
	 * <strong>This must only be called by {@link PaginatorBuilder}</strong>.
	 */
	protected void finishEmotes() {
		emotes = Collections.unmodifiableMap(emotes);
	}

	/**
	 * Retrieves {@link Logger} instance used by the library.
	 * <strong>For better maintenance, it's preferred not to use this for outside logging</strong>.
	 *
	 * @return The {@link Logger} used by Pagination Utils.
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 *
	 * Utility method to log an error at the supplied {@link LogLevel}.
	 * <strong>For better maintenance, do not use this outside of the library.</strong>.
	 *
	 * @param level The {@link LogLevel} to be used.
	 * @param msg The message to be logged.
	 * @param t The {@link Throwable} to be added for more detailed information.
	 */
	public void log(LogLevel level, String msg, Throwable t) {
		if (PUtilsConfig.getLogLevel().compareTo(level) >= 0)
			logger.error("[" + level.name().replace("_", " ") + "] " + msg, t);
	}

	/**
	 *
	 * Utility method to log an error at the supplied {@link LogLevel}.
	 * <strong>For better maintenance, do not use this outside of the library.</strong>.
	 *
	 * @param level The {@link LogLevel} to be used.
	 * @param msg The message to be logged.
	 */
	public void log(LogLevel level, String msg) {
		if (PUtilsConfig.getLogLevel().compareTo(level) >= 0)
			logger.error("[" + level.name().replace("_", " ") + "] " + msg);
	}
}
