package com.github.ygimenez.model.helper;

import com.github.ygimenez.exception.NullPageException;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.ygimenez.type.Emote.*;

public class PaginateHelper extends BaseHelper<PaginateHelper, List<Page>> {
	private int skipAmount = 0;
	private boolean fastForward = false;

	public PaginateHelper(boolean useButtons) {
		super(PaginateHelper.class, new ArrayList<>(), useButtons);
	}

	public PaginateHelper(List<Page> pages, boolean useButtons) {
		super(PaginateHelper.class, pages, useButtons);
	}

	public PaginateHelper addPage(Page page) {
		getContent().add(page);
		return this;
	}

	public int getSkipAmount() {
		return skipAmount;
	}

	public PaginateHelper setSkipAmount(int skipAmount) {
		this.skipAmount = skipAmount;
		return this;
	}

	public boolean isFastForward() {
		return fastForward;
	}

	public PaginateHelper setFastForward(boolean fastForward) {
		this.fastForward = fastForward;
		return this;
	}

	@Override
	public MessageAction apply(MessageAction action) {
		if (!isUsingButtons()) return action;

		if (getContent().isEmpty()) throw new NullPageException();
		InteractPage p = (InteractPage) getContent().get(0);

		List<ActionRow> rows = new ArrayList<>();

		LinkedList<Component> row = new LinkedList<>() {{
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

		return action.setActionRows(rows);
	}

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
