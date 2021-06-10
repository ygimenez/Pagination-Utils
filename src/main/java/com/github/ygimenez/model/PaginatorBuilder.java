package com.github.ygimenez.model;

import com.github.ygimenez.exception.InvalidHandlerException;
import com.github.ygimenez.exception.InvalidStateException;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.type.ButtonOp;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.annotation.Nonnull;

public class PaginatorBuilder {
	private final Paginator paginator;

	private PaginatorBuilder(@Nonnull Paginator paginator) {
		this.paginator = paginator;
	}
	
	public static PaginatorBuilder createPaginator() {
		return new PaginatorBuilder(new Paginator());
	}

	public static Paginator createSimplePaginator(@Nonnull Object handler) {
		Paginator p = new Paginator(handler);
		p.finishEmotes();

		return p;
	}

	public PaginatorBuilder setHandler(@Nonnull Object handler) throws InvalidHandlerException {
		if (!(handler instanceof JDA) && !(handler instanceof ShardManager))
			throw new InvalidHandlerException();

		paginator.setHandler(handler);
		return this;
	}

	public PaginatorBuilder shouldRemoveOnReact(boolean shouldRemove) {
		paginator.setRemoveOnReact(shouldRemove);
		return this;
	}
	
	public PaginatorBuilder shouldEventLock(boolean shouldLock) {
		paginator.setEventLocked(shouldLock);
		return this;
	}

	public PaginatorBuilder setDeleteOnCancel(boolean deleteOnCancel) {
		paginator.setDeleteOnCancel(deleteOnCancel);
		return this;
	}
	
	public PaginatorBuilder setEmote(@Nonnull ButtonOp buttonOp, @Nonnull Style style) {
		paginator.getEmotes().put(buttonOp, style);
		return this;
	}

	public Paginator build() {
		if (paginator.getHandler() == null)
			throw new InvalidStateException();

		paginator.finishEmotes();
		return paginator;
	}
	
	public void activate() throws InvalidHandlerException {
		if (paginator.getHandler() == null)
			throw new InvalidStateException();

		paginator.finishEmotes();
		Pages.activate(paginator);
	}
}
