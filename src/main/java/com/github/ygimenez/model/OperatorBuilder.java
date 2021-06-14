package com.github.ygimenez.model;

import com.github.ygimenez.exception.InsufficientSlotsException;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.type.ButtonOp;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.Button;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class OperatorBuilder {
	private final Operator op;

	private OperatorBuilder(Message msg) {
		op = new Operator(msg);
	}

	public static OperatorBuilder of(Message message) {
		return new OperatorBuilder(message);
	}

	public OperatorBuilder assertThat(BiPredicate<Message, User> validation) {
		BiPredicate<Message, User> current = op.getValidation();
		if (current == null)
			op.setValidation(validation);
		else
			op.setValidation(current.and(validation));

		return this;
	}

	public OperatorBuilder onClose(Consumer<Message> action) {
		op.setOnClose(action);

		return this;
	}

	public OperatorBuilder addPage(Object content) {
		op.getPages().add(new Page(content));

		return this;
	}

	public OperatorBuilder addPage(Object content, String group) {
		op.getPages().add(new Page(content, group));

		return this;
	}

	public OperatorBuilder addPages(Page... pages) {
		op.getPages().addAll(List.of(pages));

		return this;
	}

	public OperatorBuilder addButton(ButtonOp type, int index, Button button, BiConsumer<Message, User> action) {
		if (op.getButtonSlots() == 0) throw new InsufficientSlotsException();
		op.getButtons().add(new Action(button, type, action, index));

		return this;
	}

	public OperatorBuilder addButton(ButtonOp type, Button button, BiConsumer<Message, User> action) {
		if (op.getButtonSlots() == 0) throw new InsufficientSlotsException();
		op.getButtons().add(new Action(button, type, action));

		return this;
	}

	public OperatorBuilder addButton(Button button, BiConsumer<Message, User> action) {
		if (op.getButtonSlots() == 0) throw new InsufficientSlotsException();
		op.getButtons().add(new Action(button, ButtonOp.CUSTOM, action));

		return this;
	}

	public OperatorBuilder addButtons(Action... buttons) {
		if (op.getButtonSlots() - buttons.length < 0) throw new InsufficientSlotsException();
		op.getButtons().addAll(List.of(buttons));

		return this;
	}

	public OperatorBuilder enableFlag(Pages.Mode mode, Operator.Flag flag) {
		if (flag.getMode() != mode) {
			throw new IllegalArgumentException("Flags can only be enabled to their own mode.");
		}
		op.getFlags().put(mode, flag);

		return this;
	}

	public OperatorBuilder disableFlag(Pages.Mode mode) {
		op.getFlags().remove(mode);

		return this;
	}

	public OperatorBuilder setTimeout(int time, TimeUnit unit) {
		op.setTimeout(time, unit);

		return this;
	}

	public OperatorBuilder cancellable() {
		op.showCancel();

		return this;
	}

	public Operator build() {
		return op;
	}
}
