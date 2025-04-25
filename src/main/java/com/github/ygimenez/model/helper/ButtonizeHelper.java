package com.github.ygimenez.model.helper;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.*;
import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.ygimenez.type.Emote.CANCEL;

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
	 * Creates a new buttonize event helper with the supplied mapping. Utility constructor for {@link String}-mapped buttons.
	 *
	 * @param buttons A {@link TextMapping} containing the initial categories.
	 * @param useButtons Whether to use interaction buttons or legacy reaction-based buttons.
	 */
	public ButtonizeHelper(@NotNull TextMapping<ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons) {
		this(buttons.toMap(), useButtons);
	}

	/**
	 * Creates a new buttonize event helper with the supplied mapping. Utility constructor for {@link Emoji}-mapped buttons.
	 *
	 * @param buttons An {@link EmojiMapping} containing the initial categories.
	 * @param useButtons Whether to use interaction buttons or legacy reaction-based buttons.
	 */
	public ButtonizeHelper(@NotNull EmojiMapping<ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons) {
		this(buttons.toMap(), useButtons);
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
		getContent().put(new EmojiId(emoji, style), action);
		return this;
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
		getContent().put(new TextId(label, style), action);
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
	public <Out extends MessageRequest<Out>> List<LayoutComponent> getComponents(Out action) {
		if (!isUsingButtons()) return List.of();

		List<LayoutComponent> rows = new ArrayList<>();

		List<ItemComponent> row = new ArrayList<>();
		for (ButtonId<?> k : getContent().keySet()) {
			if (row.size() == 5) {
				rows.add(ActionRow.of(row));
				row = new ArrayList<>();
			}

			if (k instanceof TextId) {
				String id = k.extractId();
				row.add(Button.of(k.getStyle(), id, id));
			} else {
				Emoji id = ((EmojiId) k).getId();
				row.add(Button.of(k.getStyle(), k.extractId(), id));
			}
		}

		boolean hasCancel = getContent().keySet().stream().anyMatch(b -> Objects.equals(b.getId(), Pages.getPaginator().getEmoji(CANCEL)));
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

		Predicate<Set<String>> checks = ids -> !isCancellable() || ids.contains(Emote.getId(Pages.getPaginator().getEmoji(CANCEL)));
		checks = checks.and(ids -> {
			for (ButtonId<?> id : getContent().keySet()) {
				String key;

				if (id instanceof EmojiId) {
					key = Emote.getId(((EmojiId) id).getId());
				} else {
					key = String.valueOf(id.getId());
				}

				if (!ids.contains(key)) return false;
			}

			return true;
		});

		Set<String> ids = msg.getButtons().stream()
				.map(Button::getId)
				.collect(Collectors.toSet());

		return !checks.test(ids);
	}
}
