package com.github.ygimenez.model;

public class Paginator {
	private Object handler;
	private boolean removeOnReact;

	public Object getHandler() {
		return handler;
	}

	protected void setHandler(Object handler) {
		this.handler = handler;
	}

	public boolean isRemoveOnReact() {
		return removeOnReact;
	}

	protected void setRemoveOnReact(boolean removeOnReact) {
		this.removeOnReact = removeOnReact;
	}
}
