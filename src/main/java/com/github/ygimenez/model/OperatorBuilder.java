package com.github.ygimenez.model;

import com.github.ygimenez.exception.InsufficientSlotsException;
import com.github.ygimenez.method.Pages;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.Button;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class OperatorBuilder {
	private final Operator op;

	private OperatorBuilder(Message msg) {
		op = new Operator(msg);
	}

	public static OperatorBuilder of(Message message) {
		return new OperatorBuilder(message);
	}

	public OperatorBuilder assertThat(Function<Message, Predicate<Message>> validation) {
		Predicate<Message> current = op.getValidation();
		if (current == null)
			op.setValidation(validation.apply(op.getMessage()));
		else
			op.setValidation(current.and(validation.apply(op.getMessage())));

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

	public OperatorBuilder addPage(Object content, int group) {
		op.getPages().add(new Page(content, group));

		return this;
	}

	public OperatorBuilder addPages(Page... pages) {
		op.getPages().addAll(List.of(pages));

		return this;
	}

	public OperatorBuilder addButton(Button button,Consumer<Message> action) {
		if (op.getButtonSlots() == 0) throw new InsufficientSlotsException();
		op.getButtons().add(new Action(button, action));

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
