package com.kuuhaku.method;

import com.kuuhaku.exception.EmptyPageCollectionException;
import com.kuuhaku.type.PageType;
import com.kuuhaku.listener.MessageListener;
import com.kuuhaku.model.Page;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.kuuhaku.type.Emote.*;

public class Pages {

	/**
	 * Adds navigation buttons to the specified Message/MessageEmbed which will
	 * navigate through a given List of pages. You must specify how long the
	 * listener will stay active before shutting down itself after a no-activity
	 * interval.
	 * 
	 * @param api   The bot's instantiated object.
	 * @param msg   The message sent which will be paginated.
	 * @param pages The pages to be shown. The order of the array will define the
	 *              order of the pages.
	 * @param time  The time before the listener automatically stop listening for
	 *              further events. (Recommended: 60)
	 * @param unit  The time's time unit. (Recommended: TimeUnit.SECONDS)
	 * 
	 * @throws ErrorResponseException Thrown if the message no longer exists or
	 *                                cannot be acessed when triggering a
	 *                                GenericMessageReactionEvent
	 */
	public static void paginate(JDA api, Message msg, List<Page> pages, int time, TimeUnit unit)
			throws ErrorResponseException {
		msg.addReaction(PREVIOUS.getCode()).queue();
		msg.addReaction(CANCEL.getCode()).queue();
		msg.addReaction(NEXT.getCode()).queue();
		api.addEventListener(new MessageListener() {
			private final int maxP = pages.size() - 1;
			private int p = 0;
			private Future<?> timeout;
			private final Consumer<Void> success = s -> api.removeEventListener(this);

			@Override
			public void onGenericMessageReaction(@Nonnull GenericMessageReactionEvent event) {
				if (timeout == null)
					timeout = msg.clearReactions().queueAfter(time, unit, success);
				if (event.getUser().isBot())
					return;

				timeout.cancel(true);
				timeout = msg.clearReactions().queueAfter(time, unit, success);
				if (event.getReactionEmote().getName().equals(PREVIOUS.getCode())) {
					if (p > 0) {
						p--;
						Page pg = pages.get(p);

						updatePage(msg, pg);
					}
				} else if (event.getReactionEmote().getName().equals(NEXT.getCode())) {
					if (p < maxP) {
						p++;
						Page pg = pages.get(p);

						updatePage(msg, pg);
					}
				} else if (event.getReactionEmote().getName().equals(CANCEL.getCode())) {
					msg.clearReactions().queue(success);
				}
			}

			@Override
			public void onMessageDelete(@Nonnull MessageDeleteEvent event) {
				if (event.getMessageId().equals(msg.getId())) {
					timeout.cancel(true);
					timeout = null;
				}
			}
		});
	}

	/**
	 * Adds menu-like buttons to the specified Message/MessageEmbed which will
	 * browse through a given Map of pages. You may specify one Page per button,
	 * adding another button with an existing unicode will ovewrite the current
	 * button's Page. You must specify how long the listener will stay active before
	 * shutting down itself after a no-activity interval.
	 * 
	 * @param api        The bot's instantiated object.
	 * @param msg        The message sent which will be categorized.
	 * @param categories The categories to be shown. The categories are defined by a
	 *                   Map containing emote unicodes as keys and Pages as values.
	 * @param time       The time before the listener automatically stop listening
	 *                   for further events. (Recommended: 60)
	 * @param unit       The time's time unit. (Recommended: TimeUnit.SECONDS)
	 * 
	 * @throws ErrorResponseException Thrown if the message no longer exists or
	 *                                cannot be acessed when triggering a
	 *                                GenericMessageReactionEvent
	 */
	public static void categorize(JDA api, Message msg, Map<String, Page> categories, int time, TimeUnit unit)
			throws ErrorResponseException {
		categories.keySet().forEach(k -> msg.addReaction(k).queue());
		msg.addReaction(CANCEL.getCode()).queue();
		api.addEventListener(new MessageListener() {
			private String currCat = "";
			private Future<?> timeout;
			private final Consumer<Void> success = s -> api.removeEventListener(this);

			@Override
			public void onGenericMessageReaction(@Nonnull GenericMessageReactionEvent event) {
				if (timeout == null)
					timeout = msg.clearReactions().queueAfter(time, unit, success);

				if (event.getUser().isBot() || event.getReactionEmote().getName().equals(currCat))
					return;
				else if (event.getReactionEmote().getName().equals(CANCEL.getCode())) {
					msg.clearReactions().queue(s -> api.removeEventListener(this));
					return;
				}

				timeout.cancel(true);
				timeout = msg.clearReactions().queueAfter(time, unit, success);

				Page pg = categories.get(event.getReactionEmote().getName());

				currCat = updateCategory(event, msg, pg);
			}

			@Override
			public void onMessageDelete(@Nonnull MessageDeleteEvent event) {
				if (event.getMessageId().equals(msg.getId())) {
					timeout.cancel(true);
					timeout = null;
				}
			}
		});
	}

	/**
	 * Adds buttons to the specified Message/MessageEmbed, with each executing a
	 * specific task on click. Each button's unicode must be unique, adding another
	 * button with an existing unicode will ovewrite the current button's Runnable.
	 * 
	 * @param api     The bot's instantiated object.
	 * @param msg     The message sent which will be buttoned.
	 * @param buttons The bottons to be shown. The buttons are defined by a Map
	 *                containing emote unicodes as keys and Runnables containing
	 *                desired behavior as values.
	 *
	 * @throws ErrorResponseException Thrown if the message no longer exists or
	 *                                cannot be acessed when triggering a
	 *                                GenericMessageReactionEvent
	 */
	public static void buttonfy(JDA api, Message msg, Map<String, BiConsumer<Member, Message>> buttons) throws ErrorResponseException {
		buttons.keySet().forEach(k -> msg.addReaction(k).queue());
		if (!buttons.containsKey(CANCEL.getCode())) msg.addReaction(CANCEL.getCode()).queue();
		api.addEventListener(new MessageListener() {

			@Override
			public void onGenericMessageReaction(@Nonnull GenericMessageReactionEvent event) {
				if (event.getUser().isBot())
					return;

				buttons.get(event.getReactionEmote().getName()).accept(event.getMember(), msg);

				if (event.getReactionEmote().getName().equals(CANCEL.getCode())) {
					msg.clearReactions().queue(s -> api.removeEventListener(this));
				}
			}

			@Override
			public void onMessageDelete(@Nonnull MessageDeleteEvent event) {
				if (event.getMessageId().equals(msg.getId())) {
					api.removeEventListener(this);
				}
			}
		});
	}

	/**
	 * Adds buttons to the specified Message/MessageEmbed, with each executing a
	 * specific task on click. Each button's unicode must be unique, adding another
	 * button with an existing unicode will ovewrite the current button's Runnable.
	 * You can specify the time in which the listener will automatically stop itself
	 * after a no-activity interval.
	 * 
	 * @param api     The bot's instantiated object.
	 * @param msg     The message sent which will be buttoned.
	 * @param buttons The bottons to be shown. The buttons are defined by a Map
	 *                containing emote unicodes as keys and Runnables containing
	 *                desired behavior as values.
	 * @param time    The time before the listener automatically stop listening for
	 *                further events. (Recommended: 60)
	 * @param unit    The time's time unit. (Recommended: TimeUnit.SECONDS)
	 *
	 * @throws ErrorResponseException Thrown if the message no longer exists or
	 *                                cannot be acessed when triggering a
	 *                                GenericMessageReactionEvent
	 */
	public static void buttonfy(JDA api, Message msg, Map<String, BiConsumer<Member, Message>> buttons, int time, TimeUnit unit)
			throws ErrorResponseException {
		buttons.keySet().forEach(k -> msg.addReaction(k).queue());
		if (!buttons.containsKey(CANCEL.getCode())) msg.addReaction(CANCEL.getCode()).queue();
		api.addEventListener(new MessageListener() {
			private Future<?> timeout;
			private final Consumer<Void> success = s -> api.removeEventListener(this);

			@Override
			public void onGenericMessageReaction(@Nonnull GenericMessageReactionEvent event) {
				if (timeout == null)
					timeout = msg.clearReactions().queueAfter(time, unit, success);

				if (event.getUser().isBot())
					return;

				buttons.get(event.getReactionEmote().getName()).accept(event.getMember(), msg);

				if (event.getReactionEmote().getName().equals(CANCEL.getCode())) {
					msg.clearReactions().queue(s -> api.removeEventListener(this));
				}

				timeout.cancel(true);
				timeout = msg.clearReactions().queueAfter(time, unit, success);
			}

			@Override
			public void onMessageDelete(@Nonnull MessageDeleteEvent event) {
				if (event.getMessageId().equals(msg.getId())) {
					timeout.cancel(true);
					timeout = null;
				}
			}
		});
	}

	private static void updatePage(Message msg, Page p) {
		if (p == null) throw new EmptyPageCollectionException();
		if (p.getType() == PageType.TEXT) {
			msg.editMessage((Message) p.getContent()).queue();
		} else {
			msg.editMessage((MessageEmbed) p.getContent()).queue();
		}
	}

	private static String updateCategory(GenericMessageReactionEvent event, Message msg, Page p) {
		AtomicReference<String> out = new AtomicReference<>("");
		if (p == null) throw new EmptyPageCollectionException();

		if (p.getType() == PageType.TEXT) {
			msg.editMessage((Message) p.getContent()).queue(s -> out.set(event.getReactionEmote().getName()));
		} else {
			msg.editMessage((MessageEmbed) p.getContent()).queue(s -> out.set(event.getReactionEmote().getName()));
		}

		return out.get();
	}
}
