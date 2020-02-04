package com.github.ygimenez.method;

import com.github.ygimenez.exception.EmptyPageCollectionException;
import com.github.ygimenez.type.PageType;
import com.github.ygimenez.listener.MessageListener;
import com.github.ygimenez.model.Page;
import com.coder4.emoji.EmojiUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.github.ygimenez.type.Emote.*;

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
				if (Objects.requireNonNull(event.getUser()).isBot() || !event.getMessageId().equals(msg.getId()))
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
				event.getReaction().removeReaction(event.getUser()).queue();
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
	 * adding another button with an existing unicode will overwrite the current
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
	 * @throws ErrorResponseException Thrown if the message no longer exists or
	 *                                cannot be acessed when triggering a
	 *                                GenericMessageReactionEvent
	 */
	public static void categorize(JDA api, Message msg, Map<String, Page> categories, int time, TimeUnit unit)
			throws ErrorResponseException {
		categories.keySet().forEach(k -> {
			if (EmojiUtils.containsEmoji(k)) msg.addReaction(k).queue();
			else msg.addReaction(Objects.requireNonNull(api.getEmoteById(k))).queue();
		});
		msg.addReaction(CANCEL.getCode()).queue();
		api.addEventListener(new MessageListener() {
			private String currCat = "";
			private Future<?> timeout;
			private final Consumer<Void> success = s -> api.removeEventListener(this);

			@Override
			public void onGenericMessageReaction(@Nonnull GenericMessageReactionEvent event) {
				if (timeout == null)
					timeout = msg.clearReactions().queueAfter(time, unit, success);

				if (Objects.requireNonNull(event.getUser()).isBot() || event.getReactionEmote().getName().equals(currCat) || !event.getMessageId().equals(msg.getId()))
					return;
				else if (event.getReactionEmote().getName().equals(CANCEL.getCode())) {
					msg.clearReactions().queue();
					return;
				}

				timeout.cancel(true);
				timeout = msg.clearReactions().queueAfter(time, unit, success);

				Page pg = categories.get(event.getReactionEmote().isEmoji() ? event.getReactionEmote().getName() : event.getReactionEmote().getId());

				currCat = updateCategory(event, msg, pg);
				event.getReaction().removeReaction(event.getUser()).queue();
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
	 * button with an existing unicode will overwrite the current button's Runnable.
	 *
	 * @param api              The bot's instantiated object.
	 * @param msg              The message sent which will be buttoned.
	 * @param buttons          The buttons to be shown. The buttons are defined by a Map
	 *                         containing emote unicodes as keys and BiConsumer<Member, Message> containing
	 *                         desired behavior as value.
	 * @param showCancelButton Should the cancel button be created automatically?
	 * @throws ErrorResponseException Thrown if the message no longer exists or
	 *                                cannot be acessed when triggering a
	 *                                GenericMessageReactionEvent
	 */
	public static void buttonize(JDA api, Message msg, Map<String, BiConsumer<Member, Message>> buttons, boolean showCancelButton) throws ErrorResponseException {
		buttons.keySet().forEach(k -> {
			if (EmojiUtils.containsEmoji(k)) msg.addReaction(k).queue();
			else msg.addReaction(Objects.requireNonNull(api.getEmoteById(k))).queue();
		});
		if (!buttons.containsKey(CANCEL.getCode()) && showCancelButton) msg.addReaction(CANCEL.getCode()).queue();
		api.addEventListener(new MessageListener() {

			@Override
			public void onGenericMessageReaction(@Nonnull GenericMessageReactionEvent event) {
				if (Objects.requireNonNull(event.getUser()).isBot() || !event.getMessageId().equals(msg.getId()))
					return;

				try {
					if (event.getReactionEmote().isEmoji()) buttons.get(event.getReactionEmote().getName()).accept(event.getMember(), msg);
					else buttons.get(event.getReactionEmote().getId()).accept(event.getMember(), msg);
				} catch (NullPointerException ignore) {
				}

				if ((!buttons.containsKey(CANCEL.getCode()) && showCancelButton) && event.getReactionEmote().getName().equals(CANCEL.getCode())) {
					msg.clearReactions().queue();
				}

				event.getReaction().removeReaction(event.getUser()).queue();
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
	 * button with an existing unicode will overwrite the current button's Runnable.
	 * You can specify the time in which the listener will automatically stop itself
	 * after a no-activity interval.
	 *
	 * @param api              The bot's instantiated object.
	 * @param msg              The message sent which will be buttoned.
	 * @param buttons          The buttons to be shown. The buttons are defined by a Map
	 *                         containing emote unicodes as keys and BiConsumer<Member, Message> containing
	 *                         desired behavior as value.
	 * @param showCancelButton Should the cancel button be created automatically?
	 * @param time             The time before the listener automatically stop listening for
	 *                         further events. (Recommended: 60)
	 * @param unit             The time's time unit. (Recommended: TimeUnit.SECONDS)
	 * @throws ErrorResponseException Thrown if the message no longer exists or
	 *                                cannot be acessed when triggering a
	 *                                GenericMessageReactionEvent
	 */
	public static void buttonize(JDA api, Message msg, Map<String, BiConsumer<Member, Message>> buttons, boolean showCancelButton, int time, TimeUnit unit)
			throws ErrorResponseException {
		buttons.keySet().forEach(k -> {
			if (EmojiUtils.containsEmoji(k)) msg.addReaction(k).queue();
			else msg.addReaction(Objects.requireNonNull(api.getEmoteById(k))).queue();
		});
		if (!buttons.containsKey(CANCEL.getCode()) && showCancelButton) msg.addReaction(CANCEL.getCode()).queue();
		api.addEventListener(new MessageListener() {
			private Future<?> timeout;
			private final Consumer<Void> success = s -> api.removeEventListener(this);

			@Override
			public void onGenericMessageReaction(@Nonnull GenericMessageReactionEvent event) {
				if (timeout == null)
					timeout = msg.clearReactions().queueAfter(time, unit, success);

				if (Objects.requireNonNull(event.getUser()).isBot() || !event.getMessageId().equals(msg.getId()))
					return;

				try {
					if (event.getReactionEmote().isEmoji()) buttons.get(event.getReactionEmote().getName()).accept(event.getMember(), msg);
					else buttons.get(event.getReactionEmote().getId()).accept(event.getMember(), msg);
				} catch (NullPointerException ignore) {

				}

				if ((!buttons.containsKey(CANCEL.getCode()) && showCancelButton) && event.getReactionEmote().getName().equals(CANCEL.getCode())) {
					msg.clearReactions().queue();
				}

				timeout.cancel(true);
				timeout = msg.clearReactions().queueAfter(time, unit, success);
				event.getReaction().removeReaction(event.getUser()).queue();
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
