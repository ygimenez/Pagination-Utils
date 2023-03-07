package com.github.ygimenez.model.helper;

import com.github.ygimenez.exception.NullPageException;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.ThrowingFunction;
import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.ygimenez.type.Emote.*;

/**
 * Helper class for building lazy-paginate events, safe for reuse.
 */
public class LazyPaginateHelper extends BaseHelper<LazyPaginateHelper, List<Page>> {
	private final ThrowingFunction<Integer, Page> pageLoader;
	private final boolean cache;

	/**
	 * Creates a new lazy-paginate event helper with the supplied page loader and default list implementation
	 * ({@link ArrayList}).
	 *
	 * @param pageLoader The lazy loader used to generate pages. The value supplied is the current page number.
	 * @param useButtons Whether to use interaction buttons or legacy reaction-based buttons.
	 */
	public LazyPaginateHelper(ThrowingFunction<Integer, Page> pageLoader, boolean useButtons) {
		super(LazyPaginateHelper.class, new ArrayList<>(), useButtons);
		this.pageLoader = pageLoader;
		this.cache = true;
		load(0);
	}

	/**
	 * Creates a new lazy-paginate event helper with the supplied page loader and a list of initially loaded pages.
	 *
	 * @param pageLoader The lazy loader used to generate pages. The value supplied is the current page number.
	 * @param initialPages A {@link List} containing the initially available pages.
	 * @param useButtons Whether to use interaction buttons or legacy reaction-based buttons.
	 */
	public LazyPaginateHelper(ThrowingFunction<Integer, Page> pageLoader, @Nullable List<Page> initialPages, boolean useButtons) {
		super(LazyPaginateHelper.class, initialPages, useButtons);
		this.pageLoader = pageLoader;
		this.cache = initialPages != null;
		load(0);
	}

	/**
	 * Adds a new page to the list.
	 *
	 * @param page The page to be added.
	 * @return The {@link LazyPaginateHelper} instance for chaining convenience.
	 */
	public LazyPaginateHelper addPage(Page page) {
		if (!cache) throw new IllegalStateException();

		getContent().add(page);
		return this;
	}

	/**
	 * Retrieves the configured page loader for this helper.
	 *
	 * @return The page loader {@link Function}
	 */
	public ThrowingFunction<Integer, Page> getPageLoader() {
		return pageLoader;
	}

	/**
	 * Loads the page represented by the specified index. Might be null, meaning there's no page available for that
	 * index.
	 *
	 * @param page The page index.
	 * @return The page returned by the loader.
	 */
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
	public <Out extends MessageRequest<Out>> List<LayoutComponent> getComponents(Out action) {
		if (!isUsingButtons()) return List.of();

		InteractPage p = (InteractPage) load(0);
		if (p == null) throw new NullPageException();

		return List.of(
				ActionRow.of(new ArrayList<>() {{
					add(p.makeButton(PREVIOUS).asDisabled());
					if (isCancellable()) add(p.makeButton(CANCEL));
					add(p.makeButton(NEXT));
				}})
		);
	}

	/** {@inheritDoc} **/
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
