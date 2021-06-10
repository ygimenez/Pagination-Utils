package com.github.ygimenez.method;

import com.github.ygimenez.exception.AlreadyActivatedException;
import com.github.ygimenez.exception.InvalidHandlerException;
import com.github.ygimenez.exception.InvalidStateException;
import com.github.ygimenez.listener.MessageHandler;
import com.github.ygimenez.model.Operator;
import com.github.ygimenez.model.Paginator;
import com.github.ygimenez.model.Style;
import com.github.ygimenez.type.ButtonOp;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Pages {
	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private static final MessageHandler handler = new MessageHandler();
	private static Paginator paginator;

	public enum Mode {
		PAGINATE, CATEGORIZE, BUTTONIZE, HYBRIDIZE
	}
	
	public static void activate(@Nonnull Paginator paginator) throws InvalidHandlerException {
		if (isActivated())
			throw new AlreadyActivatedException();

		Object hand = paginator.getHandler();
		if (hand instanceof JDA)
			((JDA) hand).addEventListener(handler);
		else if (hand instanceof ShardManager)
			((ShardManager) hand).addEventListener(handler);
		else throw new InvalidHandlerException();

		Pages.paginator = paginator;
	}
	
	public static void deactivate() {
		if (!isActivated())
			return;

		Object hand = paginator.getHandler();
		if (hand instanceof JDA)
			((JDA) hand).removeEventListener(handler);
		else if (hand instanceof ShardManager)
			((ShardManager) hand).removeEventListener(handler);

		paginator = null;
	}
	
	public static boolean isActivated() {
		return paginator != null && paginator.getHandler() != null;
	}
	
	public static Paginator getPaginator() {
		return paginator;
	}
	
	public static MessageHandler getHandler() {
		return handler;
	}

	public static void doMagic(final Operator op, final Pages.Mode mode) {
		if (!isActivated()) throw new InvalidStateException();

		List<ActionRow> rows = new ArrayList<>();

		if (mode == Mode.PAGINATE) {
			LinkedList<Component> navigator = new LinkedList<>();

			Style s = paginator.getEmotes().get(ButtonOp.CANCEL);
			navigator.add(Button.of(s.getStyle(), ButtonOp.CANCEL.name(), s.getLabel(), s.getEmoji()));

			s = paginator.getEmotes().get(ButtonOp.PREVIOUS);
			navigator.addFirst(Button.of(s.getStyle(), ButtonOp.PREVIOUS.name(), s.getLabel(), s.getEmoji()));

			s = paginator.getEmotes().get(ButtonOp.NEXT);
			navigator.addLast(Button.of(s.getStyle(), ButtonOp.NEXT.name(), s.getLabel(), s.getEmoji()));

			switch (op.getFlags().get(Mode.PAGINATE)) {
				case PAGINATE_WITH_SKIP:
					s = paginator.getEmotes().get(ButtonOp.SKIP_BACKWARD);
					navigator.addFirst(Button.of(s.getStyle(), ButtonOp.SKIP_BACKWARD.name(), s.getLabel(), s.getEmoji()));

					s = paginator.getEmotes().get(ButtonOp.SKIP_FORWARD);
					navigator.addLast(Button.of(s.getStyle(), ButtonOp.SKIP_FORWARD.name(), s.getLabel(), s.getEmoji()));
					break;
				case PAGINATE_WITH_FF:
					s = paginator.getEmotes().get(ButtonOp.GOTO_FIRST);
					navigator.addFirst(Button.of(s.getStyle(), ButtonOp.GOTO_FIRST.name(), s.getLabel(), s.getEmoji()));

					s = paginator.getEmotes().get(ButtonOp.GOTO_LAST);
					navigator.addLast(Button.of(s.getStyle(), ButtonOp.GOTO_LAST.name(), s.getLabel(), s.getEmoji()));
					break;
			}

			rows.add(ActionRow.of(navigator));
		}

		MessageAction msg = op.getMessage().editMessage(op.getMessage());
		msg.setActionRows(rows).submit();
	}
}
