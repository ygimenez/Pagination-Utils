package com.github.ygimenez.model.helper;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

abstract class BaseHelper<Sub extends BaseHelper<Sub, T>, T> {
	private final Class<Sub> subClass;

	private final T content;
	private final boolean useButtons;

	private boolean cancellable = true;
	private int time = 0;
	private TimeUnit unit = null;
	private Predicate<User> canInteract = null;

	protected BaseHelper(Class<Sub> subClass, T buttons, boolean useButtons) {
		this.subClass = subClass;
		this.content = buttons;
		this.useButtons = useButtons;
	}

	public T getContent() {
		return content;
	}

	public boolean isUsingButtons() {
		return useButtons;
	}

	public boolean isCancellable() {
		return cancellable;
	}

	public int getTime() {
		return time;
	}

	public TimeUnit getUnit() {
		return unit;
	}

	public Predicate<User> getCanInteract() {
		return canInteract;
	}

	public Sub setCancellable(boolean cancellable) {
		this.cancellable = cancellable;
		return subClass.cast(this);
	}

	public Sub setTime(int time) {
		this.time = time;
		return subClass.cast(this);
	}

	public Sub setUnit(TimeUnit unit) {
		this.unit = unit;
		return subClass.cast(this);
	}

	public Sub setTimeUnit(int time, TimeUnit unit) {
		this.time = time;
		this.unit = unit;
		return subClass.cast(this);
	}

	public Sub setCanInteract(Predicate<User> canInteract) {
		this.canInteract = canInteract;
		return subClass.cast(this);
	}

	public abstract MessageAction apply(MessageAction action);

	public abstract boolean shouldUpdate(Message msg);
}
