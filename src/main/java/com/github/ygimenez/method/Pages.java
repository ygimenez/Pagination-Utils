package com.github.ygimenez.method;

import com.github.ygimenez.exception.AlreadyActivatedException;
import com.github.ygimenez.exception.InvalidHandlerException;
import com.github.ygimenez.exception.InvalidStateException;
import com.github.ygimenez.listener.MessageHandler;
import com.github.ygimenez.model.Operator;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.Paginator;
import com.github.ygimenez.model.Style;
import com.github.ygimenez.type.ButtonOp;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

	public static void paginate(final Operator op, final int skip) {
		if (!isActivated()) throw new InvalidStateException();

		List<ActionRow> rows = new ArrayList<>();
		rows.add(ActionRow.of(getPaginationNav(op, 0)));

		op.getMessage()
				.editMessage(op.getMessage())
				.setActionRows(rows)
				.submit()
				.thenAccept(msg -> handler.addEvent(msg.getChannel().getId() + msg.getId(),
						new Consumer<>() {
							private final int maxP = op.getPages().size() - 1;
							private int p = 0;
							private final Consumer<Void> success = s -> {
								handler.removeEvent(msg);
								if (paginator.isDeleteOnCancel()) msg.delete().submit();
							};

							@Override
							public void accept(ButtonClickEvent evt) {
								String[] params = evt.getComponentId().split(Pattern.quote("."));
								String msgId = params[0];
								ButtonOp bop = ButtonOp.valueOf(params[1]);

								if (evt.getUser().isBot() || evt.getMessage() == null || !evt.getMessageId().equals(msgId))
									return;

								switch (bop) {
									case ACCEPT:
										break;
									case CANCEL:
										break;
									case NEXT:
										p = Math.min(p + 1, maxP);
										break;
									case PREVIOUS:
										p = Math.max(p - 1, 0);
										break;
									case SKIP_FORWARD:
										p = Math.min(p + skip, maxP);
										break;
									case SKIP_BACKWARD:
										p = Math.max(p - skip, 0);
										break;
									case GOTO_FIRST:
										p = 0;
										break;
									case GOTO_LAST:
										p = maxP;
										break;
								}
								rows.set(0, ActionRow.of(getPaginationNav(op, p)));

								Page pg = op.getPages().get(p);
								if (pg.getContent() instanceof Message)
									evt.editComponents(rows)
											.setContent(pg.toString())
											.submit();
								else
									evt.editComponents(rows)
											.setEmbeds((MessageEmbed) pg.getContent())
											.submit();
							}
						}));
	}

	private static List<Button> getPaginationNav(Operator op, int p) {
		LinkedList<ButtonOp> buttons = new LinkedList<>();

		if (op.isShowCancel())
			buttons.add(ButtonOp.CANCEL);

		buttons.addFirst(ButtonOp.PREVIOUS);
		buttons.addLast(ButtonOp.NEXT);

		switch (op.getFlags().get(Mode.PAGINATE)) {
			case PAGINATE_WITH_SKIP:
				buttons.addFirst(ButtonOp.SKIP_BACKWARD);

				buttons.addLast(ButtonOp.SKIP_FORWARD);
				break;
			case PAGINATE_WITH_FF:
				buttons.addFirst(ButtonOp.GOTO_FIRST);

				buttons.addLast(ButtonOp.GOTO_LAST);
				break;
		}

		return buttons.stream()
				.map(b -> {
					Style s = paginator.getEmotes().get(b);
					Button btn = Button.of(s.getStyle(), op.getMessage().getId() + "." + b.name(), s.getLabel(), s.getEmoji());

					switch (b) {
						case NEXT:
						case SKIP_FORWARD:
						case GOTO_FIRST:
							return btn.withDisabled(p == op.getPages().size() - 1);
						case PREVIOUS:
						case SKIP_BACKWARD:
						case GOTO_LAST:
							return btn.withDisabled(p == 0);
						default:
							return btn;
					}
				})
				.collect(Collectors.toList());
	}
}
