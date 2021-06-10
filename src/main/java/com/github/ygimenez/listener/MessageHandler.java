package com.github.ygimenez.listener;

import com.github.ygimenez.method.Pages;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;

public class MessageHandler extends ListenerAdapter {
	private final Map<String, Consumer<ButtonClickEvent>> events = new HashMap<>();
	private final Set<String> locks = new HashSet<>();

	public void addEvent(String id, Consumer<ButtonClickEvent> act) {
		events.put(id, act);
	}

	public void removeEvent(Message msg) {
		events.remove(msg.getChannel().getId() + msg.getId());
	}

	public Map<String, Consumer<ButtonClickEvent>> getEventMap() {
		return Collections.unmodifiableMap(events);
	}

	public void clear() {
		events.clear();
	}

	private void lock(ButtonClickEvent evt) {
		if (Pages.getPaginator().isEventLocked())
			locks.add(evt.getChannel().getId() + evt.getMessageId());
	}

	private void unlock(ButtonClickEvent evt) {
		if (Pages.getPaginator().isEventLocked())
			locks.remove(evt.getChannel().getId() + evt.getMessageId());
	}

	private boolean isLocked(ButtonClickEvent evt) {
		if (!Pages.getPaginator().isEventLocked()) return false;
		return locks.contains(evt.getChannel().getId() + evt.getMessageId());
	}

	@Override
	public void onButtonClick(@NotNull ButtonClickEvent evt) {
		User u = evt.getInteraction().getUser();
		if (u.isBot() || isLocked(evt)) return;

		try {
			lock(evt);
			Consumer<ButtonClickEvent> event = events.get(evt.getChannel().getId() + evt.getMessageId());

			if (event != null)
				event.accept(evt);
		} finally {
			unlock(evt);
		}
	}

	@Override
	public void onMessageDelete(@Nonnull MessageDeleteEvent evt) {
		events.remove(evt.getChannel().getId() + evt.getMessageId());
	}
}
