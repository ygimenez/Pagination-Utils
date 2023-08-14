package com.github.ygimenez.listener;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ActionReference;
import com.github.ygimenez.model.PUtilsConfig;
import com.github.ygimenez.model.PaginationEventWrapper;
import com.github.ygimenez.model.ThrowingBiConsumer;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;

/**
 * Class responsible for handling reaction events sent by the handler.<br>
 * Only one event is added to the handler to prevent cluttering and unnecessary listeners.
 */
public class EventHandler extends ListenerAdapter {
	private final Map<String, ThrowingBiConsumer<User, PaginationEventWrapper>> events = new ConcurrentHashMap<>();
	private final Map<String, Map<String, List<?>>> dropdownValues = new ConcurrentHashMap<>();
	private final Set<String> locks = ConcurrentHashMap.newKeySet();
	private final CRC32 crc = new CRC32();

	/**
	 * Creates a new {@link EventHandler} instance.
	 */
	public EventHandler() {
	}

	/**
	 * Adds an event to the handler, which will be executed whenever a button with the same
	 * ID is pressed.
	 *
	 * @param msg The {@link Message} to hold the event.
	 * @param act The action to be executed when the button is pressed.
	 * @return An {@link ActionReference} pointing to this event. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e. garbage collected).
	 */
	public ActionReference addEvent(@NotNull Message msg, @NotNull ThrowingBiConsumer<User, PaginationEventWrapper> act) {
		String id = getEventId(msg);
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_3, "Added event with ID " + id + " and Consumer hash " + Integer.toHexString(act.hashCode()));
		events.put(id, act);

		return new ActionReference(id);
	}

	/**
	 * Removes an event from the handler.
	 *
	 * @param msg The {@link Message} which had attached events.
	 */
	public void removeEvent(@NotNull Message msg) {
		String id = getEventId(msg);
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_3, "Removed event with ID " + id);
		events.remove(id);
		dropdownValues.remove(id);
	}

	/**
	 * Checks if an event hash is still present in the map.
	 *
	 * @param hash The event hash.
	 * @return Whether the hash exists in the events map (will be always false if hash is null).
	 */
	public boolean checkEvent(@Nullable String hash) {
		if (hash == null) return false;
		return events.containsKey(hash);
	}

	/**
	 * Retrieves the event handler map. This will contain all currently active events being handled by
	 * the library mapped by {@link MessageChannel} ID plus the {@link Message} ID.
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
		dropdownValues.clear();
	}

	private synchronized void lock(@NotNull String id) {
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Locked event with ID " + id);
		locks.add(id);
	}

	private synchronized void unlock(@NotNull String id) {
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Unlocked event with ID " + id);
		locks.remove(id);
	}

	private synchronized boolean isLocked(@NotNull GenericMessageReactionEvent evt) {
		return locks.contains(getEventId(evt));
	}

	private synchronized boolean isLocked(@NotNull String id) {
		return locks.contains(id);
	}

	@Override
	public void onMessageReactionAdd(@NotNull MessageReactionAddEvent evt) {
		execute(evt);
	}

	@Override
	public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent evt) {
		if (!Pages.getPaginator().isRemoveOnReact() || !evt.isFromGuild()) {
			execute(evt);
		}
	}

	@Override
	public void onMessageDelete(@NotNull MessageDeleteEvent evt) {
		events.remove(getEventId(evt));
	}

	private void execute(GenericMessageReactionEvent evt) {
		String id = getEventId(evt);
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Received reaction event with ID " + id);
		if (!events.containsKey(id)) {
			Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Event not mapped, skipping");
			return;
		}

		evt.retrieveUser().submit()
				.whenComplete((u, t) -> processEvent(
						t, id, u,
						new PaginationEventWrapper(evt, u, evt.getChannel(), evt.getMessageId(), evt.getReaction(), evt.isFromGuild())
				));
	}

	@Override
	public void onButtonInteraction(@NotNull ButtonInteractionEvent evt) {
		String id = getEventId(evt);
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Received button event with ID " + id);
		if (!events.containsKey(id)) {
			evt.deferEdit().submit().whenComplete((hook, t) -> PUtilsConfig.getOnRemove().accept(evt.getHook()));
			Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Event not mapped, skipping");
			return;
		}

		evt.deferEdit().submit()
				.whenComplete((hook, t) -> {
					User u = hook.getInteraction().getUser();
					processEvent(
							t, id, u,
							new PaginationEventWrapper(evt, u, evt.getChannel(), evt.getMessageId(), evt.getButton(), evt.isFromGuild())
					);
				});
	}

	private void processEvent(Throwable t, String id, User u, PaginationEventWrapper evt) {
		if (t != null) {
			Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_1, "An error occurred when processing event with ID " + id, t);
			return;
		}

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

	@Override
	public void onGenericSelectMenuInteraction(@NotNull GenericSelectMenuInteractionEvent evt) {
		String id = getEventId(evt.getMessage());
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Received dropdown values for event with ID " + id);

		dropdownValues.computeIfAbsent(id, k -> new HashMap<>())
				.put(evt.getComponentId(), evt.getValues());
	}

	public Map<String, List<?>> getDropdownValues(String eventId) {
		return dropdownValues.get(eventId);
	}

	/**
	 * Calculate internal event ID from given message. This does not mean the event exists though, but the ID will be
	 * valid if a {@link Pages} action is created for this message.
	 *
	 * @param msg Message to be calculated the ID from.
	 * @return The event ID.
	 */
	public String getEventId(Message msg) {
		crc.reset();
		String rawId = (msg.isFromGuild() ? "GUILD_" : "PRIVATE_") + msg.getChannel().getId() + "_" + msg.getId();
		crc.update(rawId.getBytes(StandardCharsets.UTF_8));

		return Long.toHexString(crc.getValue());
	}

	private String getEventId(GenericMessageEvent evt) {
		crc.reset();
		String rawId = (evt.isFromGuild() ? "GUILD_" : "PRIVATE_") + evt.getChannel().getId() + "_" + evt.getMessageId();
		crc.update(rawId.getBytes(StandardCharsets.UTF_8));

		return Long.toHexString(crc.getValue());
	}

	private String getEventId(ButtonInteractionEvent evt) {
		crc.reset();
		String rawId = (evt.isFromGuild() ? "GUILD_" : "PRIVATE_") + evt.getChannel().getId() + "_" + evt.getMessageId();
		crc.update(rawId.getBytes(StandardCharsets.UTF_8));

		return Long.toHexString(crc.getValue());
	}
}
