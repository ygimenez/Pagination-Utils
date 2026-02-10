package com.github.ygimenez.listener;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.*;
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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class responsible for handling reaction events sent by the handler.<br>
 * Only one event is added to the handler to prevent cluttering and unnecessary listeners.
 */
public class EventHandler extends ListenerAdapter {
	private final Map<String, EventData<?, ?>> events = new ConcurrentHashMap<>();
	private final Set<String> locks = ConcurrentHashMap.newKeySet();

	/**
	 * Creates a new {@link EventHandler} instance.
	 */
	public EventHandler() {
	}

	/**
	 * Adds an event to the handler, which will be executed whenever a button with the same
	 * ID is pressed.
	 *
	 * @param id  The event ID.
	 * @param evt The event data containing the action to be executed when the button is pressed.
	 * @return An {@link ActionReference} pointing to this event. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
	 */
	public ActionReference addEvent(@NotNull String id, @NotNull EventData<?, ?> evt) {
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Added event with ID " + id);
		events.put(id, evt);

		return new ActionReference(id);
	}

	/**
	 * Removes an event from the handler.
	 *
	 * @param id The event ID.
	 */
	public void removeEvent(@NotNull String id) {
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Removed event with ID " + id);
		events.remove(id);
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
	public Map<String, EventData<?, ?>> getEventMap() {
		return Collections.unmodifiableMap(events);
	}

	/**
	 * Purge events map.<br>
	 * <br>
	 * <b>WARNING:</b> This will break <u>all</u> active pagination, use it with caution.
	 */
	public void clear() {
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Cleared all active events");
		events.clear();
	}

	private synchronized void lock(@NotNull String id) {
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Locked event with ID " + id);
		locks.add(id);
	}

	private synchronized void unlock(@NotNull String id) {
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Unlocked event with ID " + id);
		locks.remove(id);
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

		EventData<?, ?> act = events.get(id);
		if (act == null) {
			Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Event not mapped, skipping");
			return;
		}

		evt.retrieveMessage().submit().whenComplete((m, t) ->
				evt.retrieveUser().submit().whenComplete((u, thr) -> {
					InteractionData data = new InteractionData(evt.getReaction().getEmoji().getFormatted(), m, evt.getUser());
					if (u.isBot() || !act.getHelper().canInteract(data)) {
						Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Interaction not allowed by canInteract, skipping");
						return;
					}

					processEvent(
							t, id, u, new PaginationEventWrapper(
									evt, u, evt.getChannel(), m, evt.getReaction(), evt.isFromGuild()
							)
					);
				})
		);
	}

	@Override
	public void onButtonInteraction(@NotNull ButtonInteractionEvent evt) {
		String id = getEventId(evt);
		Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Received button event with ID " + id);

		EventData<?, ?> act = events.get(id);
		if (act == null) {
			evt.deferEdit().submit().whenComplete((hook, t) -> Pages.getPaginator().getOnRemove().accept(evt.getHook()));
			Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Event not mapped, skipping");
			return;
		}

		evt.deferEdit().submit().whenComplete((hook, t) -> {
			InteractionData data = new InteractionData(evt.getComponentId(), evt.getMessage(), evt.getUser());
			if (evt.getUser().isBot() || !act.getHelper().canInteract(data)) {
				Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Interaction not allowed by canInteract, skipping");
				return;
			}

			User u = hook.getInteraction().getUser();
			processEvent(t, id, u, new PaginationEventWrapper(
					evt, u, evt.getChannel(), evt.getMessage(), evt.getButton(), evt.isFromGuild()
			));
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
			EventData<?, ?> act = events.get(id);

			if (act != null) {
				Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Action found");
				act.getAction().accept(u, evt);
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

		EventData<?, ?> act = events.get(id);
		if (act == null) {
			evt.deferEdit().submit().whenComplete((hook, t) -> Pages.getPaginator().getOnRemove().accept(evt.getHook()));
			Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Event not mapped, skipping");
			return;
		}

		evt.deferEdit().submit().whenComplete((hook, t) -> {
			InteractionData data = new InteractionData(evt.getComponentId(), evt.getMessage(), evt.getUser());
			if (evt.getUser().isBot() || !act.getHelper().canInteract(data)) {
				Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Interaction not allowed by canInteract, skipping");
				return;
			}

			act.getHelper().getDropdownValues().put(data.getId(), evt.getValues());
		});
	}

	/**
	 * Calculate internal event ID from a given message. This does not mean the event exists though, but the ID will be
	 * valid if a {@link Pages} action is created for this message.
	 *
	 * @param msg Message to be calculated the ID from.
	 * @return The event ID.
	 */
	public String getEventId(Message msg) {
		return (msg.isFromGuild() ? "GUILD_" : "PRIVATE_") + msg.getChannel().getId() + "_" + msg.getId();
	}

	private String getEventId(GenericMessageEvent evt) {
		return (evt.isFromGuild() ? "GUILD_" : "PRIVATE_") + evt.getChannel().getId() + "_" + evt.getMessageId();
	}

	private String getEventId(ButtonInteractionEvent evt) {
		return (evt.isFromGuild() ? "GUILD_" : "PRIVATE_") + evt.getChannel().getId() + "_" + evt.getMessageId();
	}
}
