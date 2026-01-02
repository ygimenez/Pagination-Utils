package com.github.ygimenez.model.helper;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.*;
import com.github.ygimenez.type.Action;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.ygimenez.type.Action.CANCEL;

/**
 * Helper class for building buttonize events, safe for reuse.
 */
public class ButtonizeHelper extends BaseHelper<ButtonizeHelper, Map<ButtonId<?>, ThrowingConsumer<ButtonWrapper>>> {
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
	 * @param buttons A map containing the initial categories.
	 * @param useButtons Whether to use interaction buttons or legacy reaction-based buttons.
	 */
	public ButtonizeHelper(@NotNull Map<ButtonId<?>, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons) {
		super(ButtonizeHelper.class, buttons, useButtons);
	}

	/**
	 * Adds a new button to the map.
	 *
	 * @param emoji The emoji representing this button.
	 * @param action The action to be performed on click.
	 * @return The {@link ButtonizeHelper} instance for chaining convenience.
	 */
	public ButtonizeHelper addAction(@NotNull Emoji emoji, @NotNull ThrowingConsumer<ButtonWrapper> action) {
		return addAction(emoji, ButtonStyle.SECONDARY, action);
	}

	/**
	 * Adds a new button to the map.
	 *
	 * @param emoji The emoji representing this button.
	 * @param style The style of this button.
	 * @param action The action to be performed on click.
	 * @return The {@link ButtonizeHelper} instance for chaining convenience.
	 */
	public ButtonizeHelper addAction(@NotNull Emoji emoji, @NotNull ButtonStyle style, @NotNull ThrowingConsumer<ButtonWrapper> action) {
		return addAction(new EmojiId(emoji, style), action);
	}

	/**
	 * Adds a new button to the map.
	 *
	 * @param label The label representing this button.
	 * @param action The action to be performed on click.
	 * @return The {@link ButtonizeHelper} instance for chaining convenience.
	 */
	public ButtonizeHelper addAction(@NotNull String label, @NotNull ThrowingConsumer<ButtonWrapper> action) {
		return addAction(label, ButtonStyle.SECONDARY, action);
	}

	/**
	 * Adds a new button to the map with a specified style.
	 *
	 * @param label The label representing this button.
	 * @param style The style of this button.
	 * @param action The action to be performed on click.
	 * @return The {@link ButtonizeHelper} instance for chaining convenience.
	 */
	public ButtonizeHelper addAction(@NotNull String label, @NotNull ButtonStyle style, @NotNull ThrowingConsumer<ButtonWrapper> action) {
		return addAction(new TextId(label, style), action);
	}

	/**
	 * Adds a new button to the map with a custom {@link ButtonId}.
	 *
	 * @param buttonId The configuration for this button.
	 * @param action The action to be performed on click.
	 * @return The {@link ButtonizeHelper} instance for chaining convenience.
	 */
	public ButtonizeHelper addAction(@NotNull ButtonId<?> buttonId, @NotNull ThrowingConsumer<ButtonWrapper> action) {
		getContent().put(buttonId, action);
		return this;
	}

	/**
	 * Clear all buttons.
	 *
	 * @return The {@link ButtonizeHelper} instance for chaining convenience.
	 */
	public ButtonizeHelper clearActions() {
		getContent().clear();
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

	@Override
	public <Out extends MessageRequest<Out>> List<MessageTopLevelComponent> getComponents(Out action) {
		if (!isUsingButtons()) return List.of();

		List<MessageTopLevelComponent> rows = new ArrayList<>();

		List<ActionRowChildComponent> row = new ArrayList<>();
		for (ButtonId<?> k : getContent().keySet()) {
			if (row.size() == 5) {
				rows.add(ActionRow.of(row));
				row = new ArrayList<>();
			}

			String key = k.getId() + "." + (int) (Math.random() * Integer.MAX_VALUE);
			if (k instanceof TextId) {
				row.add(Button.of(k.getStyle(), key, ((TextId) k).getContent()));
			} else {
				row.add(Button.of(k.getStyle(), key, ((EmojiId) k).getContent()));
			}
		}

		boolean hasCancel = getContent().keySet().stream().anyMatch(b -> Objects.equals(b.getContent(), Pages.getPaginator().getEmoji(CANCEL)));
		if (!hasCancel && isCancellable()) {
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

		return rows;
	}

	/** {@inheritDoc} **/
	@Override
	public boolean shouldUpdate(Message msg) {
		if (!isUsingButtons()) return false;

		Predicate<Set<String>> checks = ids -> !isCancellable() || ids.contains(Action.getId(Pages.getPaginator().getEmoji(CANCEL)));
		checks = checks.and(ids -> {
			for (ButtonId<?> id : getContent().keySet()) {
				if (!ids.contains(id.getId())) return false;
			}

			return true;
		});

		Set<String> ids = Pages.getButtons(msg).stream()
				.map(Button::getCustomId)
				.filter(Objects::nonNull)
				.map(ButtonId.ID_PATTERN::split)
				.filter(parts -> parts.length == 2)
				.map(parts -> parts[0])
				.collect(Collectors.toSet());

		return !checks.test(ids);
	}

	@Override
	public ButtonizeHelper clone() {
		return new ButtonizeHelper(new LinkedHashMap<>(getContent()), isUsingButtons());
	}
}
