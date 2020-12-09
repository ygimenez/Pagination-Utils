package com.github.ygimenez.method;

import com.coder4.emoji.EmojiUtils;
import com.github.ygimenez.exception.AlreadyActivatedException;
import com.github.ygimenez.exception.NullPageException;
import com.github.ygimenez.exception.InvalidStateException;
import com.github.ygimenez.listener.MessageHandler;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.github.ygimenez.type.Emote.*;

public class Pages {
	public static MessageHandler handler = new MessageHandler();
	public static boolean activated;

	/**
	 * Sets a {@link JDA} object to be used as incoming reactions handler. This is
	 * required only once unless you want to use another client as the handler. <br>
	 * <br>
	 * Before calling this method again, you must use {@link #deactivate(JDA)} to
	 * remove current handler, else this method will throw
	 * {@link AlreadyActivatedException}.
	 *
	 * @param api The bot's {@link JDA} object.
	 * @throws AlreadyActivatedException Thrown if there's a handler already set.
	 */
	public static void activate(JDA api) {
		if (activated) throw new AlreadyActivatedException();
		api.addEventListener(handler);
		activated = true;
	}

	/**
	 * Remove current button handler, allowing another {@link #activate(JDA)} call.
	 *
	 * @param api The {@link JDA} object which is currently handling buttons.
	 *            Calling this method without having a handler currently set will do
	 *            nothing.
	 */
	public static void deactivate(JDA api) {
		if (!activated)
			return;
		api.removeEventListener(handler);
		activated = false;
	}

	/**
	 * Sets a {@link ShardManager} object to be used as incoming reactions handler.
	 * This is only required once unless you want to use another manager as the
	 * handler. <br>
	 * <br>
	 * Before calling this method again, you must use
	 * {@link #deactivate(ShardManager)} to remove current handler, else this method
	 * will throw {@link AlreadyActivatedException}.
	 *
	 * @param manager The bot's {@link ShardManager} object.
	 * @throws AlreadyActivatedException Thrown if there's a handler already set.
	 */
	public static void activate(ShardManager manager) {
		if (activated) throw new AlreadyActivatedException();
		manager.addEventListener(handler);
		activated = true;
	}

	/**
	 * Remove current button handler, allowing another
	 * {@link #activate(ShardManager)} call. Calling this method without having a
	 * handler currently set will do nothing.
	 *
	 * @param manager The {@link ShardManager} object which is currently handling
	 *                buttons.
	 */
	public static void deactivate(ShardManager manager) {
		if (!activated)
			return;
		manager.removeEventListener(handler);
		activated = false;
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You must specify
	 * how long the listener will stay active before shutting down itself after a
	 * no-activity interval.
	 *
	 * @param msg   The {@link Message} sent which will be paginated.
	 * @param pages The pages to be shown. The order of the {@link List} will define
	 *              the order of the pages.
	 * @param time  The time before the listener automatically stop listening for
	 *              further events (recommended: 60).
	 * @param unit  The time's {@link TimeUnit} (recommended:
	 *              {@link TimeUnit#SECONDS}).
	 * @throws ErrorResponseException Thrown if the {@link Message} no longer exists
	 *                                or cannot be accessed when triggering a
	 *                                {@link GenericMessageReactionEvent}.
	 * @throws PermissionException    Thrown if this library cannot remove reactions
	 *                                due to lack of bot permission
	 * @throws InvalidStateException  Thrown if no handler was set with
	 *                                {@link #activate(JDA)} or
	 *                                {@link #activate(ShardManager)}
	 */
	public static void paginate(Message msg, List<Page> pages, int time, TimeUnit unit) throws ErrorResponseException, PermissionException {
		if (!activated) throw new InvalidStateException();

		msg.addReaction(PREVIOUS.getCode()).submit();
		msg.addReaction(CANCEL.getCode()).submit();
		msg.addReaction(NEXT.getCode()).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<MessageReactionAddEvent>() {
			private final int maxP = pages.size() - 1;
			private int p = 0;
			private Future<?> timeout;
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			{
				timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
			}

			@Override
			public void accept(MessageReactionAddEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					if (timeout == null)
						try {
							timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
						} catch (PermissionException ignore) {
						}
					if (Objects.requireNonNull(u).isBot() || !event.getMessageId().equals(msg.getId()))
						return;

					if (timeout != null)
						timeout.cancel(true);
					try {
						timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
					} catch (PermissionException ignore) {
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
							msg.clearReactions().submit().thenAccept(success).exceptionally(s -> {
								msg.getReactions().forEach(r -> r.removeReaction().submit());
								success.accept(null);
								return null;
							});
						} catch (PermissionException e) {
							msg.getReactions().forEach(r -> r.removeReaction().submit());
							success.accept(null);
						}
					}
					try {
						event.getReaction().removeReaction(u).submit();
					} catch (PermissionException | ErrorResponseException ignore) {
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You must specify
	 * how long the listener will stay active before shutting down itself after a
	 * no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @throws ErrorResponseException Thrown if the {@link Message} no longer exists
	 *                                or cannot be accessed when triggering a
	 *                                {@link GenericMessageReactionEvent}.
	 * @throws PermissionException    Thrown if this library cannot remove reactions
	 *                                due to lack of bot permission.
	 * @throws InvalidStateException  Thrown if no handler was set with
	 *                                {@link #activate(JDA)} or
	 *                                {@link #activate(ShardManager)}.
	 */
	public static void paginate(Message msg, List<Page> pages, int time, TimeUnit unit, Predicate<User> canInteract) throws ErrorResponseException, PermissionException {
		if (!activated) throw new InvalidStateException();

		msg.addReaction(PREVIOUS.getCode()).submit();
		msg.addReaction(CANCEL.getCode()).submit();
		msg.addReaction(NEXT.getCode()).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<MessageReactionAddEvent>() {
			private final int maxP = pages.size() - 1;
			private int p = 0;
			private Future<?> timeout;
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			{
				timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
			}

			@Override
			public void accept(MessageReactionAddEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					if (canInteract.test(u)) {
						if (timeout == null)
							try {
								timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
							} catch (PermissionException ignore) {
							}
						if (Objects.requireNonNull(u).isBot() || !event.getMessageId().equals(msg.getId()))
							return;

						if (timeout != null)
							timeout.cancel(true);
						try {
							timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
						} catch (PermissionException ignore) {
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
								msg.clearReactions().submit().thenAccept(success).exceptionally(s -> {
									msg.getReactions().forEach(r -> r.removeReaction().submit());
									success.accept(null);
									return null;
								});
							} catch (PermissionException e) {
								msg.getReactions().forEach(r -> r.removeReaction().submit());
								success.accept(null);
							}
						}
						try {
							event.getReaction().removeReaction(u).submit();
						} catch (PermissionException | ErrorResponseException ignore) {
						}
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You must specify
	 * how long the listener will stay active before shutting down itself after a
	 * no-activity interval.
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pages      The pages to be shown. The order of the {@link List} will
	 *                   define the order of the pages.
	 * @param time       The time before the listener automatically stop listening
	 *                   for further events (recommended: 60).
	 * @param unit       The time's {@link TimeUnit} (recommended:
	 *                   {@link TimeUnit#SECONDS}).
	 * @param skipAmount The amount of pages to be skipped when clicking SKIP
	 *                   buttons.
	 * @throws ErrorResponseException Thrown if the {@link Message} no longer exists
	 *                                or cannot be accessed when triggering a
	 *                                {@link GenericMessageReactionEvent}.
	 * @throws PermissionException    Thrown if this library cannot remove reactions
	 *                                due to lack of bot permission
	 * @throws InvalidStateException  Thrown if no handler was set with
	 *                                {@link #activate(JDA)} or
	 *                                {@link #activate(ShardManager)}
	 */
	public static void paginate(Message msg, List<Page> pages, int time, TimeUnit unit, int skipAmount) throws ErrorResponseException, PermissionException {
		if (!activated) throw new InvalidStateException();

		msg.addReaction(SKIP_BACKWARD.getCode()).submit();
		msg.addReaction(PREVIOUS.getCode()).submit();
		msg.addReaction(CANCEL.getCode()).submit();
		msg.addReaction(NEXT.getCode()).submit();
		msg.addReaction(SKIP_FORWARD.getCode()).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<MessageReactionAddEvent>() {
			private final int maxP = pages.size() - 1;
			private int p = 0;
			private Future<?> timeout;
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			{
				timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
			}

			@Override
			public void accept(MessageReactionAddEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					if (timeout == null)
						try {
							timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
						} catch (PermissionException ignore) {
						}
					if (Objects.requireNonNull(u).isBot() || !event.getMessageId().equals(msg.getId()))
						return;

					if (timeout != null)
						timeout.cancel(true);
					try {
						timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
					} catch (PermissionException ignore) {
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
					} else if (event.getReactionEmote().getName().equals(SKIP_BACKWARD.getCode())) {
						if (p > 0) {
							p -= (p - skipAmount < 0 ? p : skipAmount);
							Page pg = pages.get(p);

							updatePage(msg, pg);
						}
					} else if (event.getReactionEmote().getName().equals(SKIP_FORWARD.getCode())) {
						if (p < maxP) {
							p += (p + skipAmount > maxP ? maxP - p : skipAmount);
							Page pg = pages.get(p);

							updatePage(msg, pg);
						}
					} else if (event.getReactionEmote().getName().equals(CANCEL.getCode())) {
						try {
							msg.clearReactions().submit().thenAccept(success).exceptionally(s -> {
								msg.getReactions().forEach(r -> r.removeReaction().submit());
								success.accept(null);
								return null;
							});
						} catch (PermissionException e) {
							msg.getReactions().forEach(r -> r.removeReaction().submit());
							success.accept(null);
						}
					}
					try {
						event.getReaction().removeReaction(u).submit();
					} catch (PermissionException | ErrorResponseException ignore) {
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You must specify
	 * how long the listener will stay active before shutting down itself after a
	 * no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param skipAmount  The amount of pages to be skipped when clicking SKIP
	 *                    buttons.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @throws ErrorResponseException Thrown if the {@link Message} no longer exists
	 *                                or cannot be accessed when triggering a
	 *                                {@link GenericMessageReactionEvent}.
	 * @throws PermissionException    Thrown if this library cannot remove reactions
	 *                                due to lack of bot permission
	 * @throws InvalidStateException  Thrown if no handler was set with
	 *                                {@link #activate(JDA)} or
	 *                                {@link #activate(ShardManager)}
	 */
	public static void paginate(Message msg, List<Page> pages, int time, TimeUnit unit, int skipAmount, Predicate<User> canInteract) throws ErrorResponseException, PermissionException {
		if (!activated) throw new InvalidStateException();

		msg.addReaction(SKIP_BACKWARD.getCode()).submit();
		msg.addReaction(PREVIOUS.getCode()).submit();
		msg.addReaction(CANCEL.getCode()).submit();
		msg.addReaction(NEXT.getCode()).submit();
		msg.addReaction(SKIP_FORWARD.getCode()).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<MessageReactionAddEvent>() {
			private final int maxP = pages.size() - 1;
			private int p = 0;
			private Future<?> timeout;
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			{
				timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
			}

			@Override
			public void accept(MessageReactionAddEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					if (canInteract.test(u)) {
						if (timeout == null)
							try {
								timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
							} catch (PermissionException ignore) {
							}
						if (Objects.requireNonNull(u).isBot() || !event.getMessageId().equals(msg.getId()))
							return;

						if (timeout != null)
							timeout.cancel(true);
						try {
							timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
						} catch (PermissionException ignore) {
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
						} else if (event.getReactionEmote().getName().equals(SKIP_BACKWARD.getCode())) {
							if (p > 0) {
								p -= (p - skipAmount < 0 ? p : skipAmount);
								Page pg = pages.get(p);

								updatePage(msg, pg);
							}
						} else if (event.getReactionEmote().getName().equals(SKIP_FORWARD.getCode())) {
							if (p < maxP) {
								p += (p + skipAmount > maxP ? maxP - p : skipAmount);
								Page pg = pages.get(p);

								updatePage(msg, pg);
							}
						} else if (event.getReactionEmote().getName().equals(CANCEL.getCode())) {
							try {
								msg.clearReactions().submit().thenAccept(success).exceptionally(s -> {
									msg.getReactions().forEach(r -> r.removeReaction().submit());
									success.accept(null);
									return null;
								});
							} catch (PermissionException e) {
								msg.getReactions().forEach(r -> r.removeReaction().submit());
								success.accept(null);
							}
						}
						try {
							event.getReaction().removeReaction(u).submit();
						} catch (PermissionException | ErrorResponseException ignore) {
						}
					}
				});
			}
		});
	}

	/**
	 * Adds menu-like buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will browse through a given {@link Map} of pages. You may only specify
	 * one {@link Page} per button, adding another button with an existing unicode
	 * will overwrite the current button's {@link Page}. You must specify how long
	 * the listener will stay active before shutting down itself after a no-activity
	 * interval.
	 *
	 * @param msg        The {@link Message} sent which will be categorized.
	 * @param categories The categories to be shown. The categories are defined by a
	 *                   {@link Map} containing emote unicodes as keys and
	 *                   {@link Pages} as values.
	 * @param time       The time before the listener automatically stop listening
	 *                   for further events (recommended: 60).
	 * @param unit       The time's {@link TimeUnit} (recommended:
	 *                   {@link TimeUnit#SECONDS}).
	 * @throws ErrorResponseException Thrown if the {@link Message} no longer exists
	 *                                or cannot be accessed when triggering a
	 *                                {@link GenericMessageReactionEvent}.
	 * @throws PermissionException    Thrown if this library cannot remove reactions
	 *                                due to lack of bot permission
	 * @throws InvalidStateException  Thrown if no handler was set with
	 *                                {@link #activate(JDA)} or
	 *                                {@link #activate(ShardManager)}
	 */
	public static void categorize(Message msg, Map<String, Page> categories, int time, TimeUnit unit) throws ErrorResponseException, PermissionException {
		if (!activated) throw new InvalidStateException();

		categories.keySet().forEach(k -> {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else
				msg.addReaction(Objects.requireNonNull(msg.getJDA().getEmoteById(k))).submit();
		});
		msg.addReaction(CANCEL.getCode()).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<MessageReactionAddEvent>() {
			private String currCat = "";
			private Future<?> timeout;
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			{
				timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
			}

			@Override
			public void accept(@Nonnull MessageReactionAddEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					if (timeout == null)
						try {
							timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
						} catch (PermissionException ignore) {
						}

					if (Objects.requireNonNull(u).isBot() || event.getReactionEmote().getName().equals(currCat) || !event.getMessageId().equals(msg.getId()))
						return;
					else if (event.getReactionEmote().getName().equals(CANCEL.getCode())) {
						try {
							msg.clearReactions().submit().thenAccept(success).exceptionally(s -> {
								msg.getReactions().forEach(r -> r.removeReaction().submit());
								success.accept(null);
								return null;
							});
						} catch (PermissionException e) {
							msg.getReactions().forEach(r -> r.removeReaction().submit());
							success.accept(null);
						}
						return;
					}

					if (timeout != null)
						timeout.cancel(true);
					try {
						timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
					} catch (PermissionException ignore) {
					}

					Page pg = categories.get(event.getReactionEmote().isEmoji() ? event.getReactionEmote().getName() : event.getReactionEmote().getId());

					currCat = updateCategory(event, msg, pg);
					try {
						event.getReaction().removeReaction(u).submit();
					} catch (PermissionException | ErrorResponseException ignore) {
					}
				});
			}
		});
	}

	/**
	 * Adds menu-like buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will browse through a given {@link Map} of pages. You may only specify
	 * one {@link Page} per button, adding another button with an existing unicode
	 * will overwrite the current button's {@link Page}. You must specify how long
	 * the listener will stay active before shutting down itself after a no-activity
	 * interval.
	 *
	 * @param msg         The {@link Message} sent which will be categorized.
	 * @param categories  The categories to be shown. The categories are defined by
	 *                    a {@link Map} containing emote unicodes as keys and
	 *                    {@link Pages} as values.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @throws ErrorResponseException Thrown if the {@link Message} no longer exists
	 *                                or cannot be accessed when triggering a
	 *                                {@link GenericMessageReactionEvent}.
	 * @throws PermissionException    Thrown if this library cannot remove reactions
	 *                                due to lack of bot permission
	 * @throws InvalidStateException  Thrown if no handler was set with
	 *                                {@link #activate(JDA)} or
	 *                                {@link #activate(ShardManager)}
	 */
	public static void categorize(Message msg, Map<String, Page> categories, int time, TimeUnit unit, Predicate<User> canInteract) throws ErrorResponseException, PermissionException {
		if (!activated) throw new InvalidStateException();

		categories.keySet().forEach(k -> {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else
				msg.addReaction(Objects.requireNonNull(msg.getJDA().getEmoteById(k))).submit();
		});
		msg.addReaction(CANCEL.getCode()).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<MessageReactionAddEvent>() {
			private String currCat = "";
			private Future<?> timeout;
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			{
				timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
			}

			@Override
			public void accept(@Nonnull MessageReactionAddEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					if (canInteract.test(u)) {
						if (timeout == null)
							try {
								timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
							} catch (PermissionException ignore) {
							}

						if (Objects.requireNonNull(u).isBot() || event.getReactionEmote().getName().equals(currCat) || !event.getMessageId().equals(msg.getId()))
							return;
						else if (event.getReactionEmote().getName().equals(CANCEL.getCode())) {
							try {
								msg.clearReactions().submit().thenAccept(success).exceptionally(s -> {
									msg.getReactions().forEach(r -> r.removeReaction().submit());
									success.accept(null);
									return null;
								});
							} catch (PermissionException e) {
								msg.getReactions().forEach(r -> r.removeReaction().submit());
								success.accept(null);
							}
							return;
						}

						if (timeout != null)
							timeout.cancel(true);
						try {
							timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
						} catch (PermissionException ignore) {
						}

						Page pg = categories.get(event.getReactionEmote().isEmoji() ? event.getReactionEmote().getName() : event.getReactionEmote().getId());

						currCat = updateCategory(event, msg, pg);
						try {
							event.getReaction().removeReaction(u).submit();
						} catch (PermissionException | ErrorResponseException ignore) {
						}
					}
				});
			}
		});
	}

	/**
	 * Adds buttons to the specified {@link Message}/{@link MessageEmbed}, with each
	 * executing a specific task on click. You may only specify one {@link Runnable}
	 * per button, adding another button with an existing unicode will overwrite the
	 * current button's {@link Runnable}.
	 *
	 * @param msg              The {@link Message} sent which will be buttoned.
	 * @param buttons          The buttons to be shown. The buttons are defined by a
	 *                         {@link Map} containing emote unicodes as keys and
	 *                         {@link BiConsumer}<{@link Member}, {@link Message}>}
	 *                         containing desired behavior as value.
	 * @param showCancelButton Should the CANCEL button be created automatically?
	 * @throws ErrorResponseException Thrown if the {@link Message} no longer exists
	 *                                or cannot be accessed when triggering a
	 *                                {@link GenericMessageReactionEvent}.
	 * @throws PermissionException    Thrown if this library cannot remove reactions
	 *                                due to lack of bot permission
	 * @throws InvalidStateException  Thrown if no handler was set with
	 *                                {@link #activate(JDA)} or
	 *                                {@link #activate(ShardManager)}
	 */
	public static void buttonize(Message msg, Map<String, BiConsumer<Member, Message>> buttons, boolean showCancelButton) throws ErrorResponseException, PermissionException {
		if (!activated) throw new InvalidStateException();

		buttons.keySet().forEach(k -> {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else
				msg.addReaction(Objects.requireNonNull(msg.getJDA().getEmoteById(k))).submit();
		});
		if (!buttons.containsKey(CANCEL.getCode()) && showCancelButton)
			msg.addReaction(CANCEL.getCode()).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<MessageReactionAddEvent>() {
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			@Override
			public void accept(@Nonnull MessageReactionAddEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					if (Objects.requireNonNull(u).isBot() || !event.getMessageId().equals(msg.getId()))
						return;

					try {
						if (event.getReactionEmote().isEmoji())
							buttons.get(event.getReactionEmote().getName()).accept(event.getMember(), msg);
						else
							buttons.get(event.getReactionEmote().getId()).accept(event.getMember(), msg);
					} catch (NullPointerException ignore) {
					}

					if ((!buttons.containsKey(CANCEL.getCode()) && showCancelButton)
						&& event.getReactionEmote().getName().equals(CANCEL.getCode())) {
						try {
							msg.clearReactions().submit().thenAccept(success).exceptionally(s -> {
								msg.getReactions().forEach(r -> r.removeReaction().submit());
								success.accept(null);
								return null;
							});
						} catch (PermissionException e) {
							msg.getReactions().forEach(r -> r.removeReaction().submit());
							success.accept(null);
						}
					}

					try {
						event.getReaction().removeReaction(u).submit();
					} catch (PermissionException | ErrorResponseException ignore) {
					}
				});
			}
		});
	}

	/**
	 * Adds buttons to the specified {@link Message}/{@link MessageEmbed}, with each
	 * executing a specific task on click. You may only specify one {@link Runnable}
	 * per button, adding another button with an existing unicode will overwrite the
	 * current button's {@link Runnable}. You can specify the time in which the
	 * listener will automatically stop itself after a no-activity interval.
	 *
	 * @param msg              The {@link Message} sent which will be buttoned.
	 * @param buttons          The buttons to be shown. The buttons are defined by a
	 *                         Map containing emote unicodes as keys and
	 *                         {@link BiConsumer}<{@link Member}, {@link Message}>
	 *                         containing desired behavior as value.
	 * @param showCancelButton Should the CANCEL button be created automatically?
	 * @param time             The time before the listener automatically stop
	 *                         listening for further events (recommended: 60).
	 * @param unit             The time's {@link TimeUnit} (recommended:
	 *                         {@link TimeUnit#SECONDS}).
	 * @throws ErrorResponseException Thrown if the {@link Message} no longer exists
	 *                                or cannot be accessed when triggering a
	 *                                {@link GenericMessageReactionEvent}.
	 * @throws PermissionException    Thrown if this library cannot remove reactions
	 *                                due to lack of bot permission
	 * @throws InvalidStateException  Thrown if no handler was set with
	 *                                {@link #activate(JDA)} or
	 *                                {@link #activate(ShardManager)}
	 */
	public static void buttonize(Message msg, Map<String, BiConsumer<Member, Message>> buttons, boolean showCancelButton, int time, TimeUnit unit) throws ErrorResponseException, PermissionException {
		if (!activated) throw new InvalidStateException();

		buttons.keySet().forEach(k -> {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else
				msg.addReaction(Objects.requireNonNull(msg.getJDA().getEmoteById(k))).submit();
		});
		if (!buttons.containsKey(CANCEL.getCode()) && showCancelButton)
			msg.addReaction(CANCEL.getCode()).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<MessageReactionAddEvent>() {
			private Future<?> timeout;
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			{
				timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
			}

			@Override
			public void accept(@Nonnull MessageReactionAddEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					if (timeout == null)
						try {
							timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
						} catch (PermissionException ignore) {
						}

					if (Objects.requireNonNull(u).isBot() || !event.getMessageId().equals(msg.getId()))
						return;

					try {
						if (event.getReactionEmote().isEmoji())
							buttons.get(event.getReactionEmote().getName()).accept(event.getMember(), msg);
						else
							buttons.get(event.getReactionEmote().getId()).accept(event.getMember(), msg);
					} catch (NullPointerException ignore) {

					}

					if ((!buttons.containsKey(CANCEL.getCode()) && showCancelButton)
						&& event.getReactionEmote().getName().equals(CANCEL.getCode())) {
						try {
							msg.clearReactions().submit().thenAccept(success).exceptionally(s -> {
								msg.getReactions().forEach(r -> r.removeReaction().submit());
								success.accept(null);
								return null;
							});
						} catch (PermissionException e) {
							msg.getReactions().forEach(r -> r.removeReaction().submit());
							success.accept(null);
						}
					}

					if (timeout != null)
						timeout.cancel(true);
					try {
						timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
					} catch (PermissionException ignore) {
					}
					try {
						event.getReaction().removeReaction(u).submit();
					} catch (PermissionException | ErrorResponseException ignore) {
					}
				});
			}
		});
	}

	/**
	 * Adds buttons to the specified {@link Message}/{@link MessageEmbed}, with each
	 * executing a specific task on click. You must only specify one
	 * {@link Runnable} per button, adding another button with an existing unicode
	 * will overwrite the current button's {@link Runnable}. You can specify the
	 * time in which the listener will automatically stop itself after a no-activity
	 * interval.
	 *
	 * @param msg              The {@link Message} sent which will be buttoned.
	 * @param buttons          The buttons to be shown. The buttons are defined by a
	 *                         Map containing emote unicodes as keys and
	 *                         {@link BiConsumer}<{@link Member}, {@link Message}>
	 *                         containing desired behavior as value.
	 * @param showCancelButton Should the CANCEL button be created automatically?
	 * @param time             The time before the listener automatically stop
	 *                         listening for further events (recommended: 60).
	 * @param unit             The time's {@link TimeUnit} (recommended:
	 *                         {@link TimeUnit#SECONDS}).
	 * @param canInteract      {@link Predicate} to determine whether the
	 *                         {@link User} that pressed the button can interact
	 *                         with it or not.
	 * @throws ErrorResponseException Thrown if the {@link Message} no longer exists
	 *                                or cannot be accessed when triggering a
	 *                                {@link GenericMessageReactionEvent}.
	 * @throws PermissionException    Thrown if this library cannot remove reactions
	 *                                due to lack of bot permission
	 * @throws InvalidStateException  Thrown if no handler was set with
	 *                                {@link #activate(JDA)} or
	 *                                {@link #activate(ShardManager)}
	 */
	public static void buttonize(Message msg, Map<String, BiConsumer<Member, Message>> buttons, boolean showCancelButton, int time, TimeUnit unit, Predicate<User> canInteract) throws ErrorResponseException, PermissionException {
		if (!activated) throw new InvalidStateException();

		buttons.keySet().forEach(k -> {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else
				msg.addReaction(Objects.requireNonNull(msg.getJDA().getEmoteById(k))).submit();
		});
		if (!buttons.containsKey(CANCEL.getCode()) && showCancelButton)
			msg.addReaction(CANCEL.getCode()).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<MessageReactionAddEvent>() {
			private Future<?> timeout;
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			{
				timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
			}

			@Override
			public void accept(@Nonnull MessageReactionAddEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					if (canInteract.test(u)) {
						if (timeout == null)
							try {
								timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
							} catch (PermissionException ignore) {
							}

						if (Objects.requireNonNull(u).isBot() || !event.getMessageId().equals(msg.getId()))
							return;

						try {
							if (event.getReactionEmote().isEmoji())
								buttons.get(event.getReactionEmote().getName()).accept(event.getMember(), msg);
							else
								buttons.get(event.getReactionEmote().getId()).accept(event.getMember(), msg);
						} catch (NullPointerException ignore) {

						}

						if ((!buttons.containsKey(CANCEL.getCode()) && showCancelButton)
							&& event.getReactionEmote().getName().equals(CANCEL.getCode())) {
							try {
								msg.clearReactions().submit().thenAccept(success).exceptionally(s -> {
									msg.getReactions().forEach(r -> r.removeReaction().submit());
									success.accept(null);
									return null;
								});
							} catch (PermissionException e) {
								msg.getReactions().forEach(r -> r.removeReaction().submit());
								success.accept(null);
							}
						}

						if (timeout != null)
							timeout.cancel(true);
						try {
							timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
						} catch (PermissionException ignore) {
						}
						try {
							event.getReaction().removeReaction(u).submit();
						} catch (PermissionException | ErrorResponseException ignore) {
						}
					}
				});
			}
		});
	}

	/**
	 * Adds buttons to the specified {@link Message}/{@link MessageEmbed}, with each
	 * executing a specific task on click. You may only specify one {@link Runnable}
	 * per button, adding another button with an existing unicode will overwrite the
	 * current button's {@link Runnable}. You can specify the time in which the
	 * listener will automatically stop itself after a no-activity interval.
	 *
	 * @param msg              The {@link Message} sent which will be buttoned.
	 * @param buttons          The buttons to be shown. The buttons are defined by a
	 *                         Map containing emote unicodes as keys and
	 *                         {@link BiConsumer}<{@link Member}, {@link Message}>
	 *                         containing desired behavior as value.
	 * @param showCancelButton Should the CANCEL button be created automatically?
	 * @param time             The time before the listener automatically stop
	 *                         listening for further events (recommended: 60).
	 * @param unit             The time's {@link TimeUnit} (recommended:
	 *                         {@link TimeUnit#SECONDS}).
	 * @param canInteract      {@link Predicate} to determine whether the
	 *                         {@link User} that pressed the button can interact
	 *                         with it or not.
	 * @param onCancel         Action to be ran after the listener is removed.
	 * @throws ErrorResponseException Thrown if the {@link Message} no longer exists
	 *                                or cannot be accessed when triggering a
	 *                                {@link GenericMessageReactionEvent}.
	 * @throws PermissionException    Thrown if this library cannot remove reactions
	 *                                due to lack of bot permission.
	 * @throws InvalidStateException  Thrown if no handler was set with
	 *                                {@link #activate(JDA)} or
	 *                                {@link #activate(ShardManager)}.
	 */
	public static void buttonize(Message msg, Map<String, BiConsumer<Member, Message>> buttons, boolean showCancelButton, int time, TimeUnit unit, Predicate<User> canInteract, Consumer<Message> onCancel) throws ErrorResponseException, PermissionException {
		if (!activated) throw new InvalidStateException();

		buttons.keySet().forEach(k -> {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else
				msg.addReaction(Objects.requireNonNull(msg.getJDA().getEmoteById(k))).submit();
		});
		if (!buttons.containsKey(CANCEL.getCode()) && showCancelButton)
			msg.addReaction(CANCEL.getCode()).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<MessageReactionAddEvent>() {
			private Future<?> timeout;
			private final Consumer<Void> success = s -> {
				handler.removeEvent(msg);
				if (onCancel != null)
					onCancel.accept(msg);
			};

			{
				timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
			}

			@Override
			public void accept(@Nonnull MessageReactionAddEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					if (canInteract.test(u)) {
						if (timeout == null)
							try {
								timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
							} catch (PermissionException ignore) {
							}

						if (Objects.requireNonNull(u).isBot() || !event.getMessageId().equals(msg.getId()))
							return;

						try {
							if (event.getReactionEmote().isEmoji())
								buttons.get(event.getReactionEmote().getName()).accept(event.getMember(), msg);
							else
								buttons.get(event.getReactionEmote().getId()).accept(event.getMember(), msg);
						} catch (NullPointerException ignore) {

						}

						if ((!buttons.containsKey(CANCEL.getCode()) && showCancelButton)
							&& event.getReactionEmote().getName().equals(CANCEL.getCode())) {
							try {
								msg.clearReactions().submit().thenAccept(success).exceptionally(s -> {
									msg.getReactions().forEach(r -> r.removeReaction().submit());
									success.accept(null);
									return null;
								});
							} catch (PermissionException e) {
								msg.getReactions().forEach(r -> r.removeReaction().submit());
								success.accept(null);
							}
						}

						if (timeout != null)
							timeout.cancel(true);
						try {
							timeout = msg.clearReactions().submitAfter(time, unit).thenAccept(success);
						} catch (PermissionException ignore) {
						}
						try {
							event.getReaction().removeReaction(u).submit();
						} catch (PermissionException | ErrorResponseException ignore) {
						}
					}
				});
			}
		});
	}

	private static void updatePage(Message msg, Page p) {
		if (p == null) throw new NullPageException();
		if (p.getType() == PageType.TEXT) {
			msg.editMessage((Message) p.getContent()).submit();
		} else {
			msg.editMessage((MessageEmbed) p.getContent()).submit();
		}
	}

	private static String updateCategory(GenericMessageReactionEvent event, Message msg, Page p) {
		AtomicReference<String> out = new AtomicReference<>("");
		if (p == null) throw new NullPageException();

		if (p.getType() == PageType.TEXT) {
			msg.editMessage((Message) p.getContent()).submit()
					.thenAccept(s -> out.set(event.getReactionEmote().getName()));
		} else {
			msg.editMessage((MessageEmbed) p.getContent()).submit()
					.thenAccept(s -> out.set(event.getReactionEmote().getName()));
		}

		return out.get();
	}
}
