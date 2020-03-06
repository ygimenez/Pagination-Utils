package com.github.ygimenez.method;

import com.coder4.emoji.EmojiUtils;
import com.github.ygimenez.exception.EmptyPageCollectionException;
import com.github.ygimenez.listener.MessageListener;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.PermissionException;

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
	 * @throws PermissionException    Thrown if this library cannot remove reactions due to lack of bot permission
	 */
	public static void paginate(JDA api, Message msg, List<Page> pages, int time, TimeUnit unit)
			throws ErrorResponseException, PermissionException {
		msg.addReaction(PREVIOUS.getCode()).queue();
		msg.addReaction(CANCEL.getCode()).queue();
		msg.addReaction(NEXT.getCode()).queue();
		api.addEventListener(new MessageListener() {
			private final int maxP = pages.size() - 1;
			private int p = 0;
			private Future<?> timeout;
			private final Consumer<Void> success = s -> api.removeEventListener(this);

			@Override
			public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
				if (timeout == null)
					try {
						timeout = msg.clearReactions().queueAfter(time, unit, success, Pages::doNothing);
					} catch (InsufficientPermissionException ignore) {
					}
				if (Objects.requireNonNull(event.getUser()).isBot() || !event.getMessageId().equals(msg.getId()))
					return;

				if (timeout != null) timeout.cancel(true);
				try {
					timeout = msg.clearReactions().queueAfter(time, unit, success, Pages::doNothing);
				} catch (InsufficientPermissionException ignore) {
				}
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
					try {
						msg.clearReactions().queue(success, Pages::doNothing);
					} catch (InsufficientPermissionException e) {
						msg.getReactions().forEach(r -> r.removeReaction(api.getSelfUser()).queue());
						success.accept(null);
					}
				}
				try {
					event.getReaction().removeReaction(event.getUser()).queue(null, Pages::doNothing);
				} catch (InsufficientPermissionException ignore) {
				}
			}

			@Override
			public void onMessageReactionRemove(@Nonnull MessageReactionRemoveEvent event) {
				if (Objects.requireNonNull(event.getUser()).isBot() || !event.getMessageId().equals(msg.getId()))
					return;

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
				}
			}

			@Override
			public void onMessageDelete(@Nonnull MessageDeleteEvent event) {
				if (event.getMessageId().equals(msg.getId()) && timeout != null) {
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
	 * @throws PermissionException    Thrown if this library cannot remove reactions due to lack of bot permission
	 */
	public static void categorize(JDA api, Message msg, Map<String, Page> categories, int time, TimeUnit unit)
			throws ErrorResponseException, PermissionException {
		categories.keySet().forEach(k -> {
			if (EmojiUtils.containsEmoji(k)) msg.addReaction(k).queue(null, Pages::doNothing);
			else msg.addReaction(Objects.requireNonNull(api.getEmoteById(k))).queue(null, Pages::doNothing);
		});
		msg.addReaction(CANCEL.getCode()).queue(null, Pages::doNothing);
		api.addEventListener(new MessageListener() {
			private String currCat = "";
			private Future<?> timeout;
			private final Consumer<Void> success = s -> api.removeEventListener(this);

			@Override
			public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
				if (timeout == null)
					try {
						timeout = msg.clearReactions().queueAfter(time, unit, success, Pages::doNothing);
					} catch (InsufficientPermissionException ignore) {
					}

				if (Objects.requireNonNull(event.getUser()).isBot() || event.getReactionEmote().getName().equals(currCat) || !event.getMessageId().equals(msg.getId()))
					return;
				else if (event.getReactionEmote().getName().equals(CANCEL.getCode())) {
					try {
						msg.clearReactions().queue(success, Pages::doNothing);
					} catch (InsufficientPermissionException e) {
						msg.getReactions().forEach(r -> r.removeReaction(api.getSelfUser()).queue());
						success.accept(null);
					}
					return;
				}

				if (timeout != null) timeout.cancel(true);
				try {
					timeout = msg.clearReactions().queueAfter(time, unit, success, Pages::doNothing);
				} catch (InsufficientPermissionException ignore) {
				}

				Page pg = categories.get(event.getReactionEmote().isEmoji() ? event.getReactionEmote().getName() : event.getReactionEmote().getId());

				currCat = updateCategory(event, msg, pg);
				try {
					event.getReaction().removeReaction(event.getUser()).queue(null, Pages::doNothing);
				} catch (InsufficientPermissionException ignore) {
				}
			}

			@Override
			public void onMessageReactionRemove(@Nonnull MessageReactionRemoveEvent event) {
				if (Objects.requireNonNull(event.getUser()).isBot() || !event.getMessageId().equals(msg.getId()))
					return;

				Page pg = categories.get(event.getReactionEmote().isEmoji() ? event.getReactionEmote().getName() : event.getReactionEmote().getId());

				currCat = updateCategory(event, msg, pg);
			}

			@Override
			public void onMessageDelete(@Nonnull MessageDeleteEvent event) {
				if (event.getMessageId().equals(msg.getId()) && timeout != null) {
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
	 * @throws PermissionException    Thrown if this library cannot remove reactions due to lack of bot permission
	 */
	public static void buttonize(JDA api, Message msg, Map<String, BiConsumer<Member, Message>> buttons, boolean showCancelButton) throws ErrorResponseException, PermissionException {
		buttons.keySet().forEach(k -> {
			if (EmojiUtils.containsEmoji(k)) msg.addReaction(k).queue(null, Pages::doNothing);
			else msg.addReaction(Objects.requireNonNull(api.getEmoteById(k))).queue(null, Pages::doNothing);
		});
		if (!buttons.containsKey(CANCEL.getCode()) && showCancelButton)
			msg.addReaction(CANCEL.getCode()).queue(null, Pages::doNothing);
		api.addEventListener(new MessageListener() {
			private final Consumer<Void> success = s -> api.removeEventListener(this);

			@Override
			public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
				if (Objects.requireNonNull(event.getUser()).isBot() || !event.getMessageId().equals(msg.getId()))
					return;

				try {
					if (event.getReactionEmote().isEmoji())
						buttons.get(event.getReactionEmote().getName()).accept(event.getMember(), msg);
					else buttons.get(event.getReactionEmote().getId()).accept(event.getMember(), msg);
				} catch (NullPointerException ignore) {
				}

				if ((!buttons.containsKey(CANCEL.getCode()) && showCancelButton) && event.getReactionEmote().getName().equals(CANCEL.getCode())) {
					try {
						msg.clearReactions().queue(success, Pages::doNothing);
					} catch (InsufficientPermissionException e) {
						msg.getReactions().forEach(r -> r.removeReaction(api.getSelfUser()).queue());
						success.accept(null);
					}
				}

				try {
					event.getReaction().removeReaction(event.getUser()).queue(null, Pages::doNothing);
				} catch (InsufficientPermissionException ignore) {
				}
			}

			@Override
			public void onMessageReactionRemove(@Nonnull MessageReactionRemoveEvent event) {
				if (Objects.requireNonNull(event.getUser()).isBot() || !event.getMessageId().equals(msg.getId()))
					return;

				try {
					if (event.getReactionEmote().isEmoji())
						buttons.get(event.getReactionEmote().getName()).accept(event.getMember(), msg);
					else buttons.get(event.getReactionEmote().getId()).accept(event.getMember(), msg);
				} catch (NullPointerException ignore) {
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
	 * @throws PermissionException    Thrown if this library cannot remove reactions due to lack of bot permission
	 */
	public static void buttonize(JDA api, Message msg, Map<String, BiConsumer<Member, Message>> buttons, boolean showCancelButton, int time, TimeUnit unit)
			throws ErrorResponseException, PermissionException {
		buttons.keySet().forEach(k -> {
			if (EmojiUtils.containsEmoji(k)) msg.addReaction(k).queue(null, Pages::doNothing);
			else msg.addReaction(Objects.requireNonNull(api.getEmoteById(k))).queue(null, Pages::doNothing);
		});
		if (!buttons.containsKey(CANCEL.getCode()) && showCancelButton)
			msg.addReaction(CANCEL.getCode()).queue(null, Pages::doNothing);
		api.addEventListener(new MessageListener() {
			private Future<?> timeout;
			private final Consumer<Void> success = s -> api.removeEventListener(this);

			@Override
			public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
				if (timeout == null)
					try {
						timeout = msg.clearReactions().queueAfter(time, unit, success, Pages::doNothing);
					} catch (InsufficientPermissionException ignore) {
					}

				if (Objects.requireNonNull(event.getUser()).isBot() || !event.getMessageId().equals(msg.getId()))
					return;

				try {
					if (event.getReactionEmote().isEmoji())
						buttons.get(event.getReactionEmote().getName()).accept(event.getMember(), msg);
					else buttons.get(event.getReactionEmote().getId()).accept(event.getMember(), msg);
				} catch (NullPointerException ignore) {

				}

				if ((!buttons.containsKey(CANCEL.getCode()) && showCancelButton) && event.getReactionEmote().getName().equals(CANCEL.getCode())) {
					try {
						msg.clearReactions().queue(success, Pages::doNothing);
					} catch (InsufficientPermissionException e) {
						msg.getReactions().forEach(r -> r.removeReaction(api.getSelfUser()).queue());
						success.accept(null);
					}
				}


				if (timeout != null) timeout.cancel(true);
				try {
					timeout = msg.clearReactions().queueAfter(time, unit, success, Pages::doNothing);
				} catch (InsufficientPermissionException ignore) {
				}
				try {
					event.getReaction().removeReaction(event.getUser()).queue(null, Pages::doNothing);
				} catch (InsufficientPermissionException ignore) {
				}
			}

			@Override
			public void onMessageReactionRemove(@Nonnull MessageReactionRemoveEvent event) {
				if (Objects.requireNonNull(event.getUser()).isBot() || !event.getMessageId().equals(msg.getId()))
					return;

				try {
					if (event.getReactionEmote().isEmoji())
						buttons.get(event.getReactionEmote().getName()).accept(event.getMember(), msg);
					else buttons.get(event.getReactionEmote().getId()).accept(event.getMember(), msg);
				} catch (NullPointerException ignore) {
				}
			}

			@Override
			public void onMessageDelete(@Nonnull MessageDeleteEvent event) {
				if (event.getMessageId().equals(msg.getId()) && timeout != null) {
					timeout.cancel(true);
					timeout = null;
				}
			}
		});
	}

	private static void updatePage(Message msg, Page p) {
		if (p == null) throw new EmptyPageCollectionException();
		if (p.getType() == PageType.TEXT) {
			msg.editMessage((Message) p.getContent()).queue(null, Pages::doNothing);
		} else {
			msg.editMessage((MessageEmbed) p.getContent()).queue(null, Pages::doNothing);
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

	private static void doNothing(Throwable t) {
		try {
			throw t;
		} catch (Throwable ignore) {
		}
	}
}
