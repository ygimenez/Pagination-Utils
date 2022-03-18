package com.github.ygimenez.model.helper;

import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.ThrowingFunction;
import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.List;

public class LazyPaginateHelper extends PaginateHelper {
	private final ThrowingFunction<Integer, Page> pageLoader;

	public LazyPaginateHelper(Message message, ThrowingFunction<Integer, Page> pageLoader, boolean useButtons) {
		super(message, new ArrayList<>(), useButtons);
		this.pageLoader = pageLoader;
	}

	public LazyPaginateHelper(Message message, ThrowingFunction<Integer, Page> pageLoader, List<Page> initialPages, boolean useButtons) {
		super(message, initialPages, useButtons);
		this.pageLoader = pageLoader;
	}

	public LazyPaginateHelper addPage(Page page) {
		getContent().add(page);
		return this;
	}

	public ThrowingFunction<Integer, Page> getPageLoader() {
		return pageLoader;
	}

	public LazyPaginateHelper load(int page) {
		getContent().set(page, pageLoader.apply(page));
		return this;
	}
}
