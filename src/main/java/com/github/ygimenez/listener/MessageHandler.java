package com.github.ygimenez.listener;

import com.github.ygimenez.method.Pages;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.CRC32;

/**
 * Class responsible for handling reaction events sent by the handler.<br>
 * Only one event is added to the handler to prevent cluttering and unnecessary listeners.
 */
public class MessageHandler extends ListenerAdapter {
	private final Map<String, Consumer<GenericMessageReactionEvent>> events = new HashMap<>();
	private final Set<String> locks = new HashSet<>();

	/**
	 * <strong>DEPRECATED:</strong> Please use {@link #addEvent(Message, Consumer)} instead.<br>
	 * <br>
	 * Adds an event to the handler, which will be executed whenever a button with the same
	 * ID is pressed.
	 *
	 * @param id  The ID of the event.
	 * @param act The action to be executed when the button is pressed.
	 */
	@Deprecated(since = "2.3.0", forRemoval = true)
	public void addEvent(String id, Consumer<GenericMessageReactionEvent> act) {
		events.put(id, act);
	}

	/**
	 * Adds an event to the handler, which will be executed whenever a button with the same
	 * ID is pressed.
	 *
	 * @param msg The {@link Message} to hold the event.
	 * @param act The action to be executed when the button is pressed.
	 */
	public void addEvent(Message msg, Consumer<GenericMessageReactionEvent> act) {
		events.put(getEventId(msg), act);
	}

	/**
	 * Removes an event from the handler.
	 *
	 * @param msg The {@link Message} which had attached events.
	 */
	public void removeEvent(Message msg) {
		events.remove(getEventId(msg));
	}

	/**
	 * Retrieves the event handler map. This will contain all currently active events being handled by
	 * the library mapped by {@link Guild} ID ({@link PrivateChannel} ID for DM) plus the {@link Message} ID.
	 *
	 * @return An unmodifiable {@link Map} containing events handled by the library.
	 */
	public Map<String, Consumer<GenericMessageReactionEvent>> getEventMap() {
		return Collections.unmodifiableMap(events);
	}

	/**
	 * Purge events map.<br>
	 * <br>
	 * <b>WARNING:</b> This will break <u>all</u> active paginations, use with caution.
	 */
	public void clear() {
		events.clear();
	}

	private void lock(GenericMessageReactionEvent evt) {
		locks.add(getEventId(evt));
	}

	private void unlock(GenericMessageReactionEvent evt) {
		locks.remove(getEventId(evt));
	}

	private boolean isLocked(GenericMessageReactionEvent evt) {
		return locks.contains(getEventId(evt));
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
		evt.retrieveUser().submit().thenAccept(u -> {
			if (u.isBot() || isLocked(evt)) return;

			try {
				if (Pages.getPaginator().isEventLocked()) lock(evt);

				Consumer<GenericMessageReactionEvent> act = events.get(getEventId(evt));

				if (act != null) act.accept(evt);
			} catch (RuntimeException e) {
				Pages.getPaginator().getLogger().error("An error occurred when processing event with ID " + getEventId(evt), e);
			} finally {
				if (Pages.getPaginator().isEventLocked()) unlock(evt);
			}
		});
	}

	private static String getEventId(GenericMessageEvent evt) {
		CRC32 crc = new CRC32();
		String rawId = (evt.isFromGuild() ? "GUILD_" + evt.getGuild().getId() : "PRIVATE_" + evt.getPrivateChannel().getId()) + evt.getMessageId();
		crc.update(rawId.getBytes(StandardCharsets.UTF_8));

		return Long.toHexString(crc.getValue());
	}

	private static String getEventId(Message msg) {
		CRC32 crc = new CRC32();
		String rawId = (msg.isFromGuild() ? "GUILD_" + msg.getGuild().getId() : "PRIVATE_" + msg.getPrivateChannel().getId()) + msg.getId();
		crc.update(rawId.getBytes(StandardCharsets.UTF_8));

		return Long.toHexString(crc.getValue());
	}
}
