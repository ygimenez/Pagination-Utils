package com.github.ygimenez.model.helper;

import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.ThrowingConsumer;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ButtonizeHelper extends BaseHelper<ButtonizeHelper, Map<Emoji, ThrowingConsumer<ButtonWrapper>>> {
	private Consumer<Message> onCancel = null;

	public ButtonizeHelper(Message message, boolean useButtons) {
		super(ButtonizeHelper.class, message, new LinkedHashMap<>(), useButtons);
	}

	public ButtonizeHelper(Message message, Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons) {
		super(ButtonizeHelper.class, message, buttons, useButtons);
	}

	public ButtonizeHelper addCategory(Emoji emoji, ThrowingConsumer<ButtonWrapper> action) {
		getContent().put(emoji, action);
		return this;
	}

	public Consumer<Message> getOnCancel() {
		return onCancel;
	}

	public ButtonizeHelper setOnCancel(Consumer<Message> onCancel) {
		this.onCancel = onCancel;
		return this;
	}
}
