package com.github.ygimenez.model.helper;

import com.github.ygimenez.exception.NullPageException;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.ThrowingFunction;
import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.ygimenez.type.Emote.*;

public class LazyPaginateHelper extends BaseHelper<LazyPaginateHelper, List<Page>> {
	private final ThrowingFunction<Integer, Page> pageLoader;
	private final boolean cache;

	public LazyPaginateHelper(ThrowingFunction<Integer, Page> pageLoader, boolean useButtons) {
		super(LazyPaginateHelper.class, new ArrayList<>(), useButtons);
		this.pageLoader = pageLoader;
		this.cache = true;
		load(0);
	}

	public LazyPaginateHelper(ThrowingFunction<Integer, Page> pageLoader, @Nullable List<Page> initialPages, boolean useButtons) {
		super(LazyPaginateHelper.class, initialPages, useButtons);
		this.pageLoader = pageLoader;
		this.cache = initialPages != null;
		load(0);
	}

	public LazyPaginateHelper addPage(Page page) {
		if (!cache) throw new IllegalStateException();

		getContent().add(page);
		return this;
	}

	public ThrowingFunction<Integer, Page> getPageLoader() {
		return pageLoader;
	}

	public @Nullable Page load(int page) {
		if (cache) {
			int maxIndex = getContent().size() - 1;
			while (maxIndex < page) {
				getContent().add(null);
				maxIndex++;
			}
		}

		Page p = pageLoader.apply(page);
		if (cache) getContent().set(page, p);
		return p;
	}

	@Override
	public <Out extends MessageRequest<Out>> Out apply(Out action) {
		if (!isUsingButtons()) return action;

		InteractPage p = (InteractPage) load(0);
		if (p == null) throw new NullPageException();

		return action.setComponents(ActionRow.of(new ArrayList<>() {{
			add(p.makeButton(PREVIOUS));
			if (isCancellable()) add(p.makeButton(CANCEL));
			add(p.makeButton(NEXT));
		}}));
	}

	@Override
	public boolean shouldUpdate(Message msg) {
		if (!isUsingButtons()) return true;

		Predicate<Set<Emote>> checks = e -> e.containsAll(Set.of(PREVIOUS, NEXT));
		Set<Emote> emotes = msg.getButtons().stream()
				.map(Emote::fromButton)
				.collect(Collectors.toSet());

		if (isCancellable()) {
			checks = checks.and(e -> e.contains(CANCEL));
		}

		return !checks.test(emotes);
	}
}
