package com.github.ygimenez.model;

import com.github.ygimenez.method.Pages;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class Operator {
	private final Message message;
	private final List<Page> pages = new ArrayList<>();
	private final Set<Action> buttons = new HashSet<>();
	private final EnumMap<Pages.Mode, Flag> flags = new EnumMap<>(Pages.Mode.class);

	private Page home = null;
	private BiPredicate<Message, User> validation;
	private Consumer<Message> onClose;
	private int time;
	private TimeUnit unit;
	private boolean showCancel;

	public enum Flag {
		PAGINATE_WITH_SKIP(Pages.Mode.PAGINATE),
		PAGINATE_WITH_FF(Pages.Mode.PAGINATE);

		private final Pages.Mode mode;

		Flag(Pages.Mode mode) {
			this.mode = mode;
		}

		public Pages.Mode getMode() {
			return mode;
		}
	}

	protected Operator(Message message) {
		this.message = message;
	}

	public Message getMessage() {
		return message;
	}

	public Page getHome() {
		return home;
	}

	protected void setHome(Page home) {
		this.home = home;
	}

	public BiPredicate<Message, User> getValidation() {
		return validation;
	}

	protected void setValidation(BiPredicate<Message, User> validation) {
		this.validation = validation;
	}

	public Consumer<Message> getOnClose() {
		return onClose;
	}

	protected void setOnClose(Consumer<Message> onClose) {
		this.onClose = onClose;
	}

	public List<Page> getPages() {
		return pages;
	}

	public Set<Action> getButtons() {
		return buttons;
	}

	public EnumMap<Pages.Mode, Flag> getFlags() {
		return flags;
	}

	public int getTime() {
		return time;
	}

	public TimeUnit getUnit() {
		return unit;
	}

	protected void setTimeout(int time, TimeUnit unit) {
		this.time = time;
		this.unit = unit;
	}

	public boolean isShowCancel() {
		return showCancel;
	}

	protected void showCancel() {
		this.showCancel = true;
	}
}