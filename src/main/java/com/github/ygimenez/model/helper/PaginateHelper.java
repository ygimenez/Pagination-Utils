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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.ygimenez.type.Action.*;

/**
 * Helper class for building paginate events, safe for reuse.
 */
public class PaginateHelper extends BaseHelper<PaginateHelper, List<Page>> {
	private int skipAmount = 0;
	private boolean fastForward = false;
	private final Map<ButtonId<?>, ThrowingBiConsumer<ButtonWrapper, Integer>> actions = new LinkedHashMap<>();
	private final Map<Integer, Map<ButtonId<?>, ThrowingBiConsumer<ButtonWrapper, Integer>>> pageActions = new LinkedHashMap<>();

	/**
	 * Creates a new paginate event helper with the default list implementation ({@link ArrayList}).
	 *
	 * @param useButtons Whether to use interaction buttons or legacy reaction-based buttons.
	 */
	public PaginateHelper(boolean useButtons) {
		super(PaginateHelper.class, new ArrayList<>(), useButtons);
	}

	/**
	 * Creates a new paginate event helper with the supplied list.
	 *
	 * @param pages A list containing the initial pages.
	 * @param useButtons Whether to use interaction buttons or legacy reaction-based buttons.
	 */
	public PaginateHelper(@NotNull List<Page> pages, boolean useButtons) {
		super(PaginateHelper.class, pages, useButtons);
	}

	/**
	 * Adds a new page to the list.
	 *
	 * @param page The page to be added.
	 * @return The {@link ButtonizeHelper} instance for chaining convenience.
	 */
	public PaginateHelper addPage(@NotNull Page page) {
		getContent().add(page);
		return this;
	}

	/**
	 * Clear all pages.
	 *
	 * @return The {@link PaginateHelper} instance for chaining convenience.
	 */
	public PaginateHelper clearPages() {
		getContent().clear();
		return this;
	}

	/**
	 * Adds an action available on every page. The callback receives the current zero-based page index.
	 * Use this instead of registering {@code Pages.buttonize} on the same message.
	 *
	 * @param id The button's identity and appearance.
	 * @param action The callback to execute.
	 * @return This helper.
	 */
	public PaginateHelper addAction(@NotNull ButtonId<?> id, @NotNull ThrowingBiConsumer<ButtonWrapper, Integer> action) {
		actions.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(action, "action"));
		return this;
	}

	/**
	 * Adds an action that does not need the current page index.
	 *
	 * @param id The button's identity and appearance.
	 * @param action The callback to execute.
	 * @return This helper.
	 */
	public PaginateHelper addAction(@NotNull ButtonId<?> id, @NotNull ThrowingConsumer<ButtonWrapper> action) {
		Objects.requireNonNull(action, "action");
		return addAction(id, (button, page) -> action.acceptThrows(button));
	}

	/**
	 * Adds an emoji action available on every page.
	 *
	 * @param emoji The emoji to display.
	 * @param action The callback, receiving the current zero-based page index.
	 * @return This helper.
	 */
	public PaginateHelper addAction(@NotNull Emoji emoji, @NotNull ThrowingBiConsumer<ButtonWrapper, Integer> action) {
		return addAction(new EmojiId(emoji), action);
	}

	/**
	 * Adds an emoji action that does not need the current page index.
	 *
	 * @param emoji The emoji to display.
	 * @param action The callback to execute.
	 * @return This helper.
	 */
	public PaginateHelper addAction(@NotNull Emoji emoji, @NotNull ThrowingConsumer<ButtonWrapper> action) {
		return addAction(new EmojiId(emoji), action);
	}

	/**
	 * Adds an action for one page. It overrides a global action having the same ID on that page.
	 *
	 * @param pageIndex The zero-based page index; its upper bound is checked when registering the helper.
	 * @param id The button's identity and appearance.
	 * @param action The callback, receiving the current zero-based page index.
	 * @return This helper.
	 */
	public PaginateHelper addPageAction(int pageIndex, @NotNull ButtonId<?> id,
			@NotNull ThrowingBiConsumer<ButtonWrapper, Integer> action) {
		if (pageIndex < 0) throw new IllegalArgumentException("Page index must not be negative");
		pageActions.computeIfAbsent(pageIndex, ignored -> new LinkedHashMap<>())
				.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(action, "action"));
		return this;
	}

	/**
	 * Adds a page-specific action that does not need the current page index.
	 *
	 * @param pageIndex The zero-based page index.
	 * @param id The button's identity and appearance.
	 * @param action The callback to execute.
	 * @return This helper.
	 */
	public PaginateHelper addPageAction(int pageIndex, @NotNull ButtonId<?> id,
			@NotNull ThrowingConsumer<ButtonWrapper> action) {
		Objects.requireNonNull(action, "action");
		return addPageAction(pageIndex, id, (button, page) -> action.acceptThrows(button));
	}

	/**
	 * Adds an emoji action for one page.
	 *
	 * @param pageIndex The zero-based page index.
	 * @param emoji The emoji to display.
	 * @param action The callback, receiving the current zero-based page index.
	 * @return This helper.
	 */
	public PaginateHelper addPageAction(int pageIndex, @NotNull Emoji emoji,
			@NotNull ThrowingBiConsumer<ButtonWrapper, Integer> action) {
		return addPageAction(pageIndex, new EmojiId(emoji), action);
	}

	/**
	 * Adds a page-specific emoji action that does not need the current page index.
	 *
	 * @param pageIndex The zero-based page index.
	 * @param emoji The emoji to display.
	 * @param action The callback to execute.
	 * @return This helper.
	 */
	public PaginateHelper addPageAction(int pageIndex, @NotNull Emoji emoji,
			@NotNull ThrowingConsumer<ButtonWrapper> action) {
		return addPageAction(pageIndex, new EmojiId(emoji), action);
	}

	/**
	 * Returns an immutable snapshot of actions available on every page.
	 *
	 * @return The global actions in display order.
	 */
	public Map<ButtonId<?>, ThrowingBiConsumer<ButtonWrapper, Integer>> getActions() {
		return Collections.unmodifiableMap(new LinkedHashMap<>(actions));
	}

	/**
	 * Returns the global actions combined with the selected page's overrides.
	 *
	 * @param pageIndex The zero-based page index.
	 * @return An immutable snapshot in display order.
	 */
	public Map<ButtonId<?>, ThrowingBiConsumer<ButtonWrapper, Integer>> getActions(int pageIndex) {
		if (pageIndex < 0) throw new IllegalArgumentException("Page index must not be negative");
		Map<ButtonId<?>, ThrowingBiConsumer<ButtonWrapper, Integer>> result = new LinkedHashMap<>(actions);
		Map<ButtonId<?>, ThrowingBiConsumer<ButtonWrapper, Integer>> specific = pageActions.get(pageIndex);
		if (specific != null) {
			for (Map.Entry<ButtonId<?>, ThrowingBiConsumer<ButtonWrapper, Integer>> entry : specific.entrySet()) {
				// Replace the key as well, so a page override can also change its label or emoji.
				result.remove(entry.getKey());
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return Collections.unmodifiableMap(result);
	}

	/**
	 * Reports whether global or page-specific actions are configured.
	 *
	 * @return Whether this helper combines pagination with custom actions.
	 */
	public boolean hasActions() {
		return !actions.isEmpty() || pageActions.values().stream().anyMatch(map -> !map.isEmpty());
	}

	/**
	 * Removes all global and page-specific actions.
	 *
	 * @return This helper.
	 */
	public PaginateHelper clearActions() {
		actions.clear();
		pageActions.clear();
		return this;
	}

	/**
	 * Removes the actions specific to one page, preserving the global actions.
	 *
	 * @param pageIndex The zero-based page index.
	 * @return This helper.
	 */
	public PaginateHelper clearPageActions(int pageIndex) {
		pageActions.remove(pageIndex);
		return this;
	}

	/**
	 * Retrieves the configured number of pages to be skipped on pressing {@link Action#SKIP_BACKWARD} or
	 * {@link Action#SKIP_FORWARD}.
	 *
	 * @return The configured number of pages to skip.
	 */
	public int getSkipAmount() {
		return skipAmount;
	}

	/**
	 * Set the number of pages to be skipped on pressing {@link Action#SKIP_BACKWARD} or {@link Action#SKIP_FORWARD}.
	 *
	 * @param skipAmount The number of pages to skip (default: 0).
	 * @return The {@link ButtonizeHelper} instance for chaining convenience.
	 */
	public PaginateHelper setSkipAmount(int skipAmount) {
		this.skipAmount = skipAmount;
		return this;
	}

	/**
	 * Retrives whether this helper is configured to include {@link Action#GOTO_FIRST} and {@link Action#GOTO_LAST}
	 * buttons.
	 *
	 * @return Whether to include fast-forward buttons.
	 */
	public boolean isFastForward() {
		return fastForward;
	}

	/**
	 * Set whether to include {@link Action#GOTO_FIRST} and {@link Action#GOTO_LAST} buttons for quick navigation
	 * through the pages.
	 *
	 * @param fastForward Whether to include fast-forward buttons (default: false).
	 * @return The {@link ButtonizeHelper} instance for chaining convenience.
	 */
	public PaginateHelper setFastForward(boolean fastForward) {
		this.fastForward = fastForward;
		return this;
	}

	/**
	 * Validates custom actions before a message is changed. Reaction fallback must pass {@code false},
	 * even when this helper originally requested interaction buttons.
	 *
	 * @param useButtons The effective interaction mode for the target message.
	 * @throws IllegalArgumentException If an index, identity, control type or Discord limit is invalid.
	 */
	public void validateActions(boolean useButtons) {
		if (getContent().isEmpty()) throw new IllegalArgumentException("At least one page is required");
		for (Integer index : pageActions.keySet()) {
			if (index < 0 || index >= getContent().size()) {
				throw new IllegalArgumentException("Custom action page index is outside the page list: " + index);
			}
		}

		Set<String> reservedIds = Arrays.stream(Action.values()).map(Enum::name).collect(Collectors.toSet());
		Set<Emoji> navigationEmojis = new HashSet<>();
		for (Action action : Arrays.asList(PREVIOUS, NEXT, CANCEL, SKIP_BACKWARD, SKIP_FORWARD, GOTO_FIRST, GOTO_LAST)) {
			navigationEmojis.add(Pages.getPaginator().getEmoji(action));
		}
		int navigationRows = skipAmount > 1 && fastForward ? 2 : 1;
		int navigationReactions = 2 + (isCancellable() ? 1 : 0) + (skipAmount > 1 ? 2 : 0) + (fastForward ? 2 : 0);

		for (int index = 0; index < getContent().size(); index++) {
			Map<ButtonId<?>, ThrowingBiConsumer<ButtonWrapper, Integer>> current = getActions(index);
			if (useButtons && !(getContent().get(index) instanceof InteractPage)) {
				throw new IllegalArgumentException("Interaction buttons require InteractPage content");
			}
			if (useButtons && navigationRows + (current.size() + 4) / 5 > 5) {
				throw new IllegalArgumentException("Navigation and custom actions exceed five component rows on page " + index);
			}
			if (!useButtons && navigationReactions + current.size() > 20) {
				throw new IllegalArgumentException("Navigation and custom actions exceed twenty reactions on page " + index);
			}

			Set<String> ids = new HashSet<>();
			Set<Emoji> emojis = new HashSet<>();
			for (ButtonId<?> id : current.keySet()) {
				if (id.getId() == null || id.getId().isEmpty() || reservedIds.contains(id.getId()) || !ids.add(id.getId())) {
					throw new IllegalArgumentException("Custom action ID is empty, duplicated or reserved: " + id.getId());
				}
				if (!(id instanceof TextId) && !(id instanceof EmojiId)) {
					throw new IllegalArgumentException("Custom actions require TextId or EmojiId");
				}
				if (!useButtons && !(id instanceof EmojiId)) {
					throw new IllegalArgumentException("Reaction pagination requires an EmojiId for every custom action");
				}
				if (id instanceof EmojiId) {
					Emoji emoji = ((EmojiId) id).getContent();
					if (emoji == null || navigationEmojis.contains(emoji) || (!useButtons && !emojis.add(emoji))) {
						throw new IllegalArgumentException("Custom action emoji is missing, duplicated or reserved for navigation");
					}
				}
				if (useButtons && id.getStyle() != ButtonStyle.PRIMARY && id.getStyle() != ButtonStyle.SECONDARY
						&& id.getStyle() != ButtonStyle.SUCCESS && id.getStyle() != ButtonStyle.DANGER) {
					throw new IllegalArgumentException("Custom actions require an interactive button style");
				}
			}
			// Let JDA check labels and other component invariants before any REST request is submitted.
			if (useButtons) getComponents(index);
		}
	}

	@Override
	public <Out extends MessageRequest<Out>> List<MessageTopLevelComponent> getComponents(Out action) {
		if (!isUsingButtons()) return List.of();
		return getComponents(0);
	}

	/**
	 * Builds navigation and custom action components for one page. Each rendering uses fresh IDs,
	 * allowing a registered pagination session to reject clicks on obsolete components.
	 *
	 * @param pageIndex The zero-based page index.
	 * @return The component rows for the page, or an empty list in reaction mode.
	 */
	public List<MessageTopLevelComponent> getComponents(int pageIndex) {
		if (!isUsingButtons()) return List.of();
		if (pageIndex < 0 || pageIndex >= getContent().size()) throw new IllegalArgumentException("Invalid page index");
		InteractPage p = (InteractPage) getContent().get(pageIndex);
		boolean first = pageIndex == 0;
		boolean last = pageIndex == getContent().size() - 1;

		List<MessageTopLevelComponent> rows = new ArrayList<>();

		LinkedList<ActionRowChildComponent> row = new LinkedList<>() {{
			add(p.makeButton(PREVIOUS).withDisabled(first));
			if (isCancellable()) add(p.makeButton(CANCEL));
			add(p.makeButton(NEXT).withDisabled(last));
		}};
		if (skipAmount > 1 && fastForward) {
			row.addFirst(p.makeButton(NONE));
			row.addLast(p.makeButton(NONE));
		} else if (skipAmount > 1) {
			row.addFirst(p.makeButton(SKIP_BACKWARD).withDisabled(first));
			row.addLast(p.makeButton(SKIP_FORWARD).withDisabled(last));
		} else if (fastForward) {
			row.addFirst(p.makeButton(GOTO_FIRST).withDisabled(first));
			row.addLast(p.makeButton(GOTO_LAST).withDisabled(last));
		}
		rows.add(ActionRow.of(row));

		if (skipAmount > 1 && fastForward) {
			rows.add(ActionRow.of(new ArrayList<>() {{
				add(p.makeButton(GOTO_FIRST).withDisabled(first));
				add(p.makeButton(SKIP_BACKWARD).withDisabled(first));
				if (isCancellable()) add(p.makeButton(NONE));
				add(p.makeButton(SKIP_FORWARD).withDisabled(last));
				add(p.makeButton(GOTO_LAST).withDisabled(last));
			}}));
		}

		List<ActionRowChildComponent> customRow = new ArrayList<>();
		String token = UUID.randomUUID().toString();
		int ordinal = 0;
		for (ButtonId<?> id : getActions(pageIndex).keySet()) {
			if (customRow.size() == 5) {
				rows.add(ActionRow.of(customRow));
				customRow = new ArrayList<>();
			}
			String key = "PU_ACTION_" + pageIndex + "_" + ordinal++ + "_" + token;
			customRow.add(id instanceof EmojiId
					? Button.of(id.getStyle(), key, id.getLabel(), ((EmojiId) id).getContent())
					: Button.of(id.getStyle(), key, id.getLabel()));
		}
		if (!customRow.isEmpty()) rows.add(ActionRow.of(customRow));

		return rows;
	}

	/** {@inheritDoc} **/
	@Override
	public boolean shouldUpdate(Message msg) {
		if (isUsingButtons()) return true;

		Predicate<Set<Action>> checks = e -> e.containsAll(Set.of(PREVIOUS, NEXT));
		Set<Action> actions = Pages.getButtons(msg).stream()
				.map(Action::fromButton)
				.collect(Collectors.toSet());

		if (isCancellable()) {
			checks = checks.and(e -> e.contains(CANCEL));
		}
		if (skipAmount > 1) {
			checks = checks.and(e -> e.containsAll(Set.of(SKIP_BACKWARD, SKIP_FORWARD)));
		}
		if (fastForward) {
			checks = checks.and(e -> e.containsAll(Set.of(GOTO_FIRST, GOTO_LAST)));
		}

		return !checks.test(actions);
	}

	@Override
	public PaginateHelper clone() {
		PaginateHelper copy = new PaginateHelper(new ArrayList<>(getContent()), isUsingButtons())
				.setSkipAmount(skipAmount).setFastForward(fastForward);
		copySettingsTo(copy);
		copy.actions.putAll(actions);
		for (Map.Entry<Integer, Map<ButtonId<?>, ThrowingBiConsumer<ButtonWrapper, Integer>>> entry : pageActions.entrySet()) {
			copy.pageActions.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
		}
		return copy;
	}
}
