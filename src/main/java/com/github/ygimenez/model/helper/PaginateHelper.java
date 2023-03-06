package com.github.ygimenez.model.helper;

import com.github.ygimenez.exception.NullPageException;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.utils.messages.MessageRequest;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.ygimenez.type.Emote.*;

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
	public PaginateHelper(List<Page> pages, boolean useButtons) {
		super(PaginateHelper.class, pages, useButtons);
	}

	/**
	 * Adds a new page to the list.
	 *
	 * @param page The page to be added.
	 * @return The {@link ButtonizeHelper} instance for chaining convenience.
	 */
	public PaginateHelper addPage(Page page) {
		getContent().add(page);
		return this;
	}

	/**
	 * Retrieves the configured amount of pages to be skipped on pressing {@link Emote#SKIP_BACKWARD} or
	 * {@link Emote#SKIP_FORWARD}.
	 *
	 * @return The configured amount of pages to skip.
	 */
	public int getSkipAmount() {
		return skipAmount;
	}

	/**
	 * Sets the amount of pages to be skipped on pressing {@link Emote#SKIP_BACKWARD} or {@link Emote#SKIP_FORWARD}.
	 *
	 * @param skipAmount The amount of pages to skip (default: 0).
	 * @return The {@link ButtonizeHelper} instance for chaining convenience.
	 */
	public PaginateHelper setSkipAmount(int skipAmount) {
		this.skipAmount = skipAmount;
		return this;
	}

	/**
	 * Retrives whether this helper is configured to include {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST}
	 * buttons.
	 *
	 * @return Whether to include fast-forward buttons.
	 */
	public boolean isFastForward() {
		return fastForward;
	}

	/**
	 * Sets whether to include {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST} buttons for quick navigation
	 * through the pages.
	 *
	 * @param fastForward Whether to include fast-forward buttons (default: false).
	 * @return The {@link ButtonizeHelper} instance for chaining convenience.
	 */
	public PaginateHelper setFastForward(boolean fastForward) {
		this.fastForward = fastForward;
		return this;
	}

	/** {@inheritDoc} **/
	@Override
	public <Out extends MessageRequest<Out>> Out apply(Out action) {
		if (!isUsingButtons()) return action;

		if (getContent().isEmpty()) throw new NullPageException();
		InteractPage p = (InteractPage) getContent().get(0);

		List<ActionRow> rows = new ArrayList<>();

		LinkedList<ItemComponent> row = new LinkedList<>() {{
			add(p.makeButton(PREVIOUS));
			if (isCancellable()) add(p.makeButton(CANCEL));
			add(p.makeButton(NEXT));
		}};
		if (skipAmount > 1 && fastForward) {
			row.addFirst(p.makeButton(NONE));
			row.addLast(p.makeButton(NONE));
		} else if (skipAmount > 1) {
			row.addFirst(p.makeButton(SKIP_BACKWARD));
			row.addLast(p.makeButton(SKIP_FORWARD));
		} else if (fastForward) {
			row.addFirst(p.makeButton(GOTO_FIRST));
			row.addLast(p.makeButton(GOTO_LAST));
		}
		rows.add(ActionRow.of(row));

		if (skipAmount > 1 && fastForward) {
			rows.add(ActionRow.of(new ArrayList<>() {{
				add(p.makeButton(GOTO_FIRST));
				add(p.makeButton(SKIP_BACKWARD));
				if (isCancellable()) add(p.makeButton(NONE));
				add(p.makeButton(SKIP_FORWARD));
				add(p.makeButton(GOTO_LAST));
			}}));
		}

		return action.setComponents(rows);
	}

	/** {@inheritDoc} **/
	@Override
	public boolean shouldUpdate(Message msg) {
		if (isUsingButtons()) return true;

		Predicate<Set<Emote>> checks = e -> e.containsAll(Set.of(PREVIOUS, NEXT));
		Set<Emote> emotes = msg.getButtons().stream()
				.map(Emote::fromButton)
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

		return !checks.test(emotes);
	}
}
