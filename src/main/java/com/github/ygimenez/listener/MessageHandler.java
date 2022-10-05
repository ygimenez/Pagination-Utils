package com.github.ygimenez.listener;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.PUtilsConfig;
import com.github.ygimenez.model.PaginationEventWrapper;
import com.github.ygimenez.model.ThrowingBiConsumer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;

/**
 * Class responsible for handling reaction events sent by the handler.<br>
 * Only one event is added to the handler to prevent cluttering and unnecessary listeners.
 */
public class MessageHandler extends ListenerAdapter {
	private final Map<String, ThrowingBiConsumer<User, PaginationEventWrapper>> events = new ConcurrentHashMap<>();
	private final Set<String> locks = ConcurrentHashMap.newKeySet();
	private final CRC32 crc = new CRC32();

	/**
	 * Adds an event to the handler, which will be executed whenever a button with the same
	 * ID is pressed.
	 *
	 * @param msg The {@link Message} to hold the event.
	 * @param act The action to be executed when the button is pressed.
	 * @return A {@link WeakReference} pointing to this event. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 */
	public WeakReference<String> addEvent(Message msg, ThrowingBiConsumer<User, PaginationEventWrapper> act) {
		String id = getEventId(msg);
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_3, "Added event with ID " + id + " and Consumer hash " + Integer.toHexString(act.hashCode()));
		events.put(id, act);

		return new WeakReference<>(id);
	}

	/**
	 * Removes an event from the handler.
	 *
	 * @param msg The {@link Message} which had attached events.
	 */
	public void removeEvent(Message msg) {
		String id = getEventId(msg);
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_3, "Removed event with ID " + id);
		events.remove(id);
	}

	/**
	 * Retrieves the event handler map. This will contain all currently active events being handled by
	 * the library mapped by {@link Guild} ID ({@link PrivateChannel} ID for DM) plus the {@link Message} ID.
	 *
	 * @return An unmodifiable {@link Map} containing events handled by the library.
	 */
	public Map<String, ThrowingBiConsumer<User, PaginationEventWrapper>> getEventMap() {
		return Collections.unmodifiableMap(events);
	}

	/**
	 * Purge events map.<br>
	 * <br>
	 * <b>WARNING:</b> This will break <u>all</u> active pagination, use with caution.
	 */
	public void clear() {
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_3, "Cleared all active events");
		events.clear();
	}

	private void lock(String id) {
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Locked event with ID " + id);
		locks.add(id);
	}

	private void unlock(String id) {
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Unlocked event with ID " + id);
		locks.remove(id);
	}

	private boolean isLocked(GenericMessageReactionEvent evt) {
		return locks.contains(getEventId(evt));
	}

	private boolean isLocked(String id) {
		return locks.contains(id);
	}

	@Override
	public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent evt) {
		execute(evt);
	}

	@Override
	public void onMessageReactionRemove(@Nonnull MessageReactionRemoveEvent evt) {
		if (!Pages.getPaginator().isRemoveOnReact() || !evt.isFromGuild())
			execute(evt);
	}

	@Override
	public void onMessageDelete(@Nonnull MessageDeleteEvent evt) {
		events.remove(getEventId(evt));
	}

	private void execute(GenericMessageReactionEvent evt) {
		String id = getEventId(evt);
		if (!events.containsKey(id)) return;

		evt.retrieveUser().submit()
				.whenComplete((u, t) -> processEvent(
						t,
						id,
						u,
						new PaginationEventWrapper(evt, u, evt.getChannel(), evt.getMessageId(), evt.getReaction(), evt.isFromGuild())
				));
	}

	@Override
	public void onButtonInteraction(@NotNull ButtonInteractionEvent evt) {
		User u = evt.getUser();
		String id = getEventId(evt);
		if (!events.containsKey(id)) return;

		evt.deferEdit().submit()
				.whenComplete((hook, t) -> processEvent(
						t,
						id,
						u,
						new PaginationEventWrapper(evt, u, evt.getChannel(), evt.getMessageId(), evt.getButton(), evt.isFromGuild())
				));
	}

	private void processEvent(Throwable t, String id, User u, PaginationEventWrapper evt) {
		if (t != null) {
			Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_1, "An error occurred when processing event with ID " + id, t);
			return;
		}

		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Received event with ID " + id);
		if (u.isBot() || isLocked(id)) {
			Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Event" + id + " was triggered by a bot or is locked. Ignored");
			return;
		}

		try {
			if (Pages.getPaginator().isEventLocked()) lock(id);

			Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Searching for action for event with ID " + id);
			ThrowingBiConsumer<User, PaginationEventWrapper> act = events.get(id);

			if (act != null) {
				Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Action found");
				act.accept(u, evt);
			} else {
				Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Action not found");
			}
		} catch (RuntimeException e) {
			Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_1, "An error occurred when processing event with ID " + id, e);
		} finally {
			if (Pages.getPaginator().isEventLocked()) unlock(id);
		}
	}

	private String getEventId(GenericMessageEvent evt) {
		crc.reset();
		String rawId = (evt.isFromGuild() ? "GUILD_" + evt.getGuild().getId() : "PRIVATE_" + evt.getChannel().asPrivateChannel().getId()) + "_" + evt.getMessageId();
		crc.update(rawId.getBytes(StandardCharsets.UTF_8));

		return Long.toHexString(crc.getValue());
	}

	private String getEventId(ButtonInteractionEvent evt) {
		crc.reset();
		String rawId = (evt.getGuild() != null ? "GUILD_" + evt.getGuild().getId() : "PRIVATE_" + evt.getChannel().asPrivateChannel().getId()) + "_" + evt.getMessageId();
		crc.update(rawId.getBytes(StandardCharsets.UTF_8));

		return Long.toHexString(crc.getValue());
	}

	private String getEventId(Message msg) {
		crc.reset();
		String rawId = (msg.isFromGuild() ? "GUILD_" + msg.getGuild().getId() : "PRIVATE_" + msg.getChannel().asPrivateChannel()) + "_" + msg.getId();
		crc.update(rawId.getBytes(StandardCharsets.UTF_8));

		return Long.toHexString(crc.getValue());
	}
}
