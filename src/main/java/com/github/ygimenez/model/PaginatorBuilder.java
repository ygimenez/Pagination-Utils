package com.github.ygimenez.model;

public class PaginatorBuilder {
	private final Paginator paginator;

	private PaginatorBuilder(Paginator paginator) {
		this.paginator = paginator;
	}

	public static PaginatorBuilder createPaginator() {
		return new PaginatorBuilder(new Paginator());
	}

	public Object getHandler() {
		return paginator.getHandler();
	}

	public PaginatorBuilder setHandler(Object handler) {
		paginator.setHandler(handler);
		return this;
	}

	public boolean willRemoveOnReact() {
		return paginator.isRemoveOnReact();
	}

	public PaginatorBuilder shouldRemoveOnReact(boolean shouldRemove) {
		paginator.setRemoveOnReact(shouldRemove);
		return this;
	}
}
