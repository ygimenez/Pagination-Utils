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

/**
 * Helper class for building buttonize events, safe for reuse.
 */
public class ButtonizeHelper extends BaseHelper<ButtonizeHelper, Map<Emoji, ThrowingConsumer<ButtonWrapper>>> {
	private Consumer<Message> onFinalization = null;

	/**
	 * Creates a new buttonize event helper with the default map implementation ({@link LinkedHashMap}).
	 *
	 * @param useButtons Whether to use interaction buttons or legacy reaction-based buttons.
	 */
	public ButtonizeHelper(boolean useButtons) {
		super(ButtonizeHelper.class, new LinkedHashMap<>(), useButtons);
	}

	/**
	 * Creates a new buttonize event helper with the supplied map.
	 *
	 * @param buttons A map containing the initial buttons.
	 * @param useButtons Whether to use interaction buttons or legacy reaction-based buttons.
	 */
	public ButtonizeHelper(Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons) {
		super(ButtonizeHelper.class, buttons, useButtons);
	}

	/**
	 * Adds a new button to the map.
	 *
	 * @param emoji The emoji representing this button.
	 * @param action The action to be performed on click.
	 * @return The {@link ButtonizeHelper} instance for chaining convenience.
	 */
	public ButtonizeHelper addAction(Emoji emoji, ThrowingConsumer<ButtonWrapper> action) {
		getContent().put(emoji, action);
		return this;
	}

	/**
	 * Retrieves the {@link Consumer} that'll be executed when the event ends.
	 *
	 * @return The action to be performed during finalization.
	 */
	public Consumer<Message> getOnFinalization() {
		return onFinalization;
	}

	/**
	 * Defines an action to be executed when the event finishes, either by user action or timed finalization.
	 *
	 * @param onFinalization The action to be performed.
	 * @return The {@link ButtonizeHelper} instance for chaining convenience.
	 */
	public ButtonizeHelper setOnFinalization(Consumer<Message> onFinalization) {
		this.onFinalization = onFinalization;
		return this;
	}

	/** {@inheritDoc} **/
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

	/** {@inheritDoc} **/
	@Override
	public boolean shouldUpdate(Message msg) {
		if (!isUsingButtons()) return false;

		Predicate<Set<Emoji>> checks = e -> !isCancellable() || e.contains(Pages.getPaginator().getEmoji(CANCEL));
		Set<Emoji> emojis = msg.getButtons().stream()
				.map(Button::getEmoji)
				.collect(Collectors.toSet());

		checks = checks.and(e -> e.containsAll(getContent().keySet()));

		return !checks.test(emojis);
	}
}
