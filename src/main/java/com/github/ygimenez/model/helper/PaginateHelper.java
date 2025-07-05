package com.github.ygimenez.model.helper;

import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.Action;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.ygimenez.type.Action.*;

/**
 * Helper class for building paginate events, safe for reuse.
 */
public class PaginateHelper extends BaseHelper<PaginateHelper, List<Page>> {
	private int skipAmount = 0;
	private boolean fastForward = false;

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

	@Override
	public <Out extends MessageRequest<Out>> List<LayoutComponent> getComponents(Out action) {
		if (!isUsingButtons()) return List.of();

		InteractPage p = (InteractPage) getContent().get(0);

		List<LayoutComponent> rows = new ArrayList<>();

		LinkedList<ItemComponent> row = new LinkedList<>() {{
			add(p.makeButton(PREVIOUS).asDisabled());
			if (isCancellable()) add(p.makeButton(CANCEL));
			add(p.makeButton(NEXT));
		}};
		if (skipAmount > 1 && fastForward) {
			row.addFirst(p.makeButton(NONE));
			row.addLast(p.makeButton(NONE));
		} else if (skipAmount > 1) {
			row.addFirst(p.makeButton(SKIP_BACKWARD).asDisabled());
			row.addLast(p.makeButton(SKIP_FORWARD));
		} else if (fastForward) {
			row.addFirst(p.makeButton(GOTO_FIRST).asDisabled());
			row.addLast(p.makeButton(GOTO_LAST));
		}
		rows.add(ActionRow.of(row));

		if (skipAmount > 1 && fastForward) {
			rows.add(ActionRow.of(new ArrayList<>() {{
				add(p.makeButton(GOTO_FIRST).asDisabled());
				add(p.makeButton(SKIP_BACKWARD).asDisabled());
				if (isCancellable()) add(p.makeButton(NONE));
				add(p.makeButton(SKIP_FORWARD));
				add(p.makeButton(GOTO_LAST));
			}}));
		}

		return rows;
	}

	/** {@inheritDoc} **/
	@Override
	public boolean shouldUpdate(Message msg) {
		if (isUsingButtons()) return true;

		Predicate<Set<Action>> checks = e -> e.containsAll(Set.of(PREVIOUS, NEXT));
		Set<Action> actions = msg.getButtons().stream()
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
		return new PaginateHelper(new ArrayList<>(getContent()), isUsingButtons());
	}
}
