package com.github.ygimenez.model.helper;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.ThrowingConsumer;
import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageRequest;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.ygimenez.type.Emote.CANCEL;

public class ButtonizeHelper extends BaseHelper<ButtonizeHelper, Map<Emoji, ThrowingConsumer<ButtonWrapper>>> {
	private Consumer<Message> onCancel = null;

	public ButtonizeHelper(boolean useButtons) {
		super(ButtonizeHelper.class, new LinkedHashMap<>(), useButtons);
	}

	public ButtonizeHelper(Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons) {
		super(ButtonizeHelper.class, buttons, useButtons);
	}

	public ButtonizeHelper addCategory(Emoji emoji, ThrowingConsumer<ButtonWrapper> action) {
		getContent().put(emoji, action);
		return this;
	}

	public Consumer<Message> getOnCancel() {
		return onCancel;
	}

	public ButtonizeHelper setOnCancel(Consumer<Message> onCancel) {
		this.onCancel = onCancel;
		return this;
	}

	@Override
	public <Out extends MessageRequest<Out>> Out apply(Out action) {
		if (!isUsingButtons()) return action;

		List<ActionRow> rows = new ArrayList<>();

		List<ItemComponent> row = new ArrayList<>();
		for (Emoji k : getContent().keySet()) {
			if (row.size() == 5) {
				rows.add(ActionRow.of(row));
				row = new ArrayList<>();
			}

			row.add(Button.secondary(Emote.getId(k), k));
		}

		if (!getContent().containsKey(Pages.getPaginator().getEmoji(CANCEL)) && isCancellable()) {
			Button button = Button.danger(CANCEL.name(), Pages.getPaginator().getEmoji(CANCEL));

			if (rows.size() == 5 && row.size() == 5) {
				row.set(4, button);
			} else if (row.size() == 5) {
				rows.add(ActionRow.of(row));
				row = new ArrayList<>();

				row.add(button);
			} else {
				row.add(button);
			}
		}

		rows.add(ActionRow.of(row));

		return action.setComponents(rows);
	}

	@Override
	public boolean shouldUpdate(Message msg) {
		if (!isUsingButtons()) return false;

		Predicate<Set<Emoji>> checks = e -> !isCancellable() || e.contains(Pages.getPaginator().getEmoji(CANCEL));
		Set<Emoji> emojis = msg.getButtons().stream()
				.map(Button::getEmoji)
				.collect(Collectors.toSet());

		checks = checks.and(e -> e.containsAll(getContent().keySet()));

		return checks.test(emojis);
	}
}
