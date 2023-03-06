package com.github.ygimenez.model.helper;

import com.github.ygimenez.exception.InvalidStateException;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class SendWrapper<T extends BaseHelper<T, ?>> {
	private final T helper;
	private final Op type;
	private MessageCreateAction action;

	private enum Op {
		PAGINATE, LAZY_PAGINATE, CATEGORIZE, BUTTONIZE
	}

	public SendWrapper(T helper) {
		this.helper = helper;

		if (helper instanceof LazyPaginateHelper) {
			this.type = Op.LAZY_PAGINATE;
		} else if (helper instanceof PaginateHelper) {
			this.type = Op.PAGINATE;
		} else if (helper instanceof CategorizeHelper) {
			this.type = Op.CATEGORIZE;
		} else if (helper instanceof ButtonizeHelper) {
			this.type = Op.BUTTONIZE;
		} else {
			this.type = null;
		}
	}

	public T getHelper() {
		return helper;
	}

	public Message send(MessageChannel channel) throws IllegalCallerException {
		InteractPage page;
		if (type == Op.PAGINATE) {
			PaginateHelper helper = (PaginateHelper) this.helper;
			page = (InteractPage) helper.getContent().get(0);
		} else if (type == Op.LAZY_PAGINATE) {
			LazyPaginateHelper helper = (LazyPaginateHelper) this.helper;
			Page pg = helper.getPageLoader().apply(0);
			if (pg == null) {
				throw new InvalidStateException();
			}

			page = (InteractPage) helper.getContent().get(0);
		} else {
			throw new IllegalCallerException("This method cannot be used for categories or buttons. Use '" + helper.getClass().getSimpleName() + ".apply(MessageCreateEvent)' instead.");
		}

		if (page.getContent() instanceof Message) {
			try (MessageCreateData data = MessageCreateBuilder.fromMessage((Message) page.getContent()).build()) {
				action = helper.apply(channel.sendMessage(data));
			}
		} else if (page.getContent() instanceof MessageEmbed) {
			action = helper.apply(channel.sendMessageEmbeds((MessageEmbed) page.getContent()));
		}

		return Pages.subGet(action);
	}
}
