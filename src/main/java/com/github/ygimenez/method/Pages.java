package com.github.ygimenez.method;

import com.coder4.emoji.EmojiUtils;
import com.github.ygimenez.exception.*;
import com.github.ygimenez.listener.MessageHandler;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.Paginator;
import com.github.ygimenez.model.ThrowingBiConsumer;
import com.github.ygimenez.type.Emote;
import com.github.ygimenez.type.PageType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.github.ygimenez.type.Emote.*;

/**
 * The main class containing all pagination-related methods, including but not futurely limited
 * to {@link #paginate(Message, List)}, {@link #categorize(Message, Map)} and
 * {@link #buttonize(Message, Map, boolean)}.
 */
public class Pages {
	private static final MessageHandler handler = new MessageHandler();
	private static Paginator paginator;
	private static boolean activated;

	/**
	 * Sets a {@link Paginator} object to handle incoming reactions. This is
	 * required only once unless you want to use another client as the handler. <br>
	 * <br>
	 * Before calling this method again, you must use {@link #deactivate()} to
	 * remove current {@link Paginator}, else this method will throw
	 * {@link AlreadyActivatedException}.
	 *
	 * @param paginator The {@link Paginator} object.
	 * @throws AlreadyActivatedException Thrown if there's a handler already set.
	 * @throws InvalidHandlerException   Thrown if the handler isn't either a {@link JDA}
	 *                                   or {@link ShardManager} object.
	 */
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

	/**
	 * Removes current button handler, allowing another {@link #activate(Paginator)} call. <br>
	 * <br>
	 * Using this method without activating beforehand will do nothing.
	 */
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

	/**
	 * <strong>DEPRECATED:</strong> Please use {@link #activate(Paginator)} instead.<br>
	 * <br>
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
	@Deprecated
	public static void activate(@Nonnull JDA api) {
		if (activated) throw new AlreadyActivatedException();
		api.addEventListener(handler);
		activated = true;
	}

	/**
	 * <strong>DEPRECATED:</strong> Please use {@link #deactivate()} instead. <br>
	 * <br>
	 * Removes current button handler, allowing another {@link #activate(JDA)} call.
	 *
	 * @param api The {@link JDA} object which is currently handling buttons.
	 *            Calling this method without having a handler currently set will do
	 *            nothing.
	 */
	@Deprecated
	public static void deactivate(@Nonnull JDA api) {
		if (!activated)
			return;
		api.removeEventListener(handler);
		activated = false;
	}

	/**
	 * <strong>DEPRECATED:</strong> Please use {@link #activate(Paginator)} instead. <br>
	 * <br>
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
	@Deprecated
	public static void activate(@Nonnull ShardManager manager) {
		if (activated) throw new AlreadyActivatedException();
		manager.addEventListener(handler);
		activated = true;
	}

	/**
	 * <strong>DEPRECATED:</strong> Please use {@link #deactivate()} instead. <br>
	 * <br>
	 * Removes current button handler, allowing another
	 * {@link #activate(ShardManager)} call. Calling this method without having a
	 * handler currently set will do nothing.
	 *
	 * @param manager The {@link ShardManager} object which is currently handling
	 *                buttons.
	 */
	@Deprecated
	public static void deactivate(@Nonnull ShardManager manager) {
		if (!activated)
			return;
		manager.removeEventListener(handler);
		activated = false;
	}

	/**
	 * Checks whether this library has been activated or not.
	 *
	 * @return The activation state of this library.
	 */
	public static boolean isActivated() {
		return (paginator != null && paginator.getHandler() != null) || activated;
	}

	/**
	 * Retrieves the {@link Paginator} object used to activate this library.
	 *
	 * @return The current {@link Paginator} object.
	 */
	public static Paginator getPaginator() {
		return paginator;
	}

	/**
	 * Retrieves the library's {@link MessageHandler} object.
	 *
	 * @return The {@link MessageHandler} object.
	 */
	public static MessageHandler getHandler() {
		return handler;
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg   The {@link Message} sent which will be paginated.
	 * @param pages The pages to be shown. The order of the {@link List} will define
	 *              the order of the pages.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		List<Page> pgs = Collections.unmodifiableList(pages);

		msg.addReaction(paginator.getEmotes().get(PREVIOUS)).submit();
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		msg.addReaction(paginator.getEmotes().get(NEXT)).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			@Override
			public void accept(GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
						return;

					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (checkEmote(reaction, PREVIOUS)) {
						if (p > 0) {
							p--;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, NEXT)) {
						if (p < maxP) {
							p++;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, CANCEL)) {
						try {
							if (msg.getChannel().getType().isGuild())
								msg.clearReactions().submit();
							else
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

							success.accept(null);
						} catch (InsufficientPermissionException | IllegalStateException e) {
							for (MessageReaction r : msg.getReactions()) {
								r.removeReaction().submit();
							}

							success.accept(null);
						}
					}

					if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
						event.getReaction().removeReaction(u).submit();
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You can specify
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
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int time, @Nonnull TimeUnit unit) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		List<Page> pgs = Collections.unmodifiableList(pages);

		msg.addReaction(paginator.getEmotes().get(PREVIOUS)).submit();
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		msg.addReaction(paginator.getEmotes().get(NEXT)).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final int maxP = pgs.size() - 1;
			private final AtomicReference<Future<?>> timeout = new AtomicReference<>(null);
			private int p = 0;
			private final Consumer<Void> success = s -> {
				handler.removeEvent(msg);
				if (timeout.get() != null) timeout.get().cancel(true);
				timeout.set(null);
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void accept(GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
						return;

					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (checkEmote(reaction, PREVIOUS)) {
						if (p > 0) {
							p--;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, NEXT)) {
						if (p < maxP) {
							p++;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, CANCEL)) {
						try {
							if (msg.getChannel().getType().isGuild())
								msg.clearReactions().submit();
							else
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

							success.accept(null);
						} catch (InsufficientPermissionException | IllegalStateException e) {
							for (MessageReaction r : msg.getReactions()) {
								r.removeReaction().submit();
							}

							success.accept(null);
						}
					}

					if (timeout.get() != null) timeout.get().cancel(true);
					timeout.set(null);
					setTimeout(timeout, success, msg, time, unit);

					if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
						event.getReaction().removeReaction(u).submit();
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, @Nonnull Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		List<Page> pgs = Collections.unmodifiableList(pages);

		msg.addReaction(paginator.getEmotes().get(PREVIOUS)).submit();
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		msg.addReaction(paginator.getEmotes().get(NEXT)).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			@Override
			public void accept(GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					if (canInteract.test(u)) {
						if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
							return;

						MessageReaction.ReactionEmote reaction = event.getReactionEmote();
						if (checkEmote(reaction, PREVIOUS)) {
							if (p > 0) {
								p--;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, NEXT)) {
							if (p < maxP) {
								p++;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, CANCEL)) {
							try {
								if (msg.getChannel().getType().isGuild())
									msg.clearReactions().submit();
								else
									for (MessageReaction r : msg.getReactions()) {
										r.removeReaction().submit();
									}

								success.accept(null);
							} catch (InsufficientPermissionException | IllegalStateException e) {
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

								success.accept(null);
							}
						}

						if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
							event.getReaction().removeReaction(u).submit();
						}
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You can specify
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
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int time, @Nonnull TimeUnit unit, @Nonnull Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		List<Page> pgs = Collections.unmodifiableList(pages);

		msg.addReaction(paginator.getEmotes().get(PREVIOUS)).submit();
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		msg.addReaction(paginator.getEmotes().get(NEXT)).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private final AtomicReference<Future<?>> timeout = new AtomicReference<>(null);
			private final Consumer<Void> success = s -> {
				handler.removeEvent(msg);
				if (timeout.get() != null) timeout.get().cancel(true);
				timeout.set(null);
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void accept(GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					if (canInteract.test(u)) {
						if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
							return;

						MessageReaction.ReactionEmote reaction = event.getReactionEmote();
						if (checkEmote(reaction, PREVIOUS)) {
							if (p > 0) {
								p--;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, NEXT)) {
							if (p < maxP) {
								p++;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, CANCEL)) {
							try {
								if (msg.getChannel().getType().isGuild())
									msg.clearReactions().submit();
								else
									for (MessageReaction r : msg.getReactions()) {
										r.removeReaction().submit();
									}

								success.accept(null);
							} catch (InsufficientPermissionException | IllegalStateException e) {
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

								success.accept(null);
							}
						}

						if (timeout.get() != null) timeout.get().cancel(true);
						timeout.set(null);
						setTimeout(timeout, success, msg, time, unit);

						if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
							event.getReaction().removeReaction(u).submit();
						}
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param fastForward Whether the {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST} buttons should be shown.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, boolean fastForward) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		List<Page> pgs = Collections.unmodifiableList(pages);

		if (fastForward) msg.addReaction(paginator.getEmotes().get(GOTO_FIRST)).submit();
		msg.addReaction(paginator.getEmotes().get(PREVIOUS)).submit();
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		msg.addReaction(paginator.getEmotes().get(NEXT)).submit();
		if (fastForward) msg.addReaction(paginator.getEmotes().get(GOTO_LAST)).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private final Consumer<Void> success = s -> handler.removeEvent(msg);


			@Override
			public void accept(GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
						return;

					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (checkEmote(reaction, PREVIOUS)) {
						if (p > 0) {
							p--;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, NEXT)) {
						if (p < maxP) {
							p++;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, GOTO_FIRST)) {
						if (p > 0) {
							p = 0;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, GOTO_LAST)) {
						if (p < maxP) {
							p = maxP;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, CANCEL)) {
						try {
							if (msg.getChannel().getType().isGuild())
								msg.clearReactions().submit();
							else
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

							success.accept(null);
						} catch (InsufficientPermissionException | IllegalStateException e) {
							for (MessageReaction r : msg.getReactions()) {
								r.removeReaction().submit();
							}

							success.accept(null);
						}
					}

					if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
						event.getReaction().removeReaction(u).submit();
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You can specify
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
	 * @param fastForward Whether the {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST} buttons should be shown.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int time, @Nonnull TimeUnit unit, boolean fastForward) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		List<Page> pgs = Collections.unmodifiableList(pages);

		if (fastForward) msg.addReaction(paginator.getEmotes().get(GOTO_FIRST)).submit();
		msg.addReaction(paginator.getEmotes().get(PREVIOUS)).submit();
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		msg.addReaction(paginator.getEmotes().get(NEXT)).submit();
		if (fastForward) msg.addReaction(paginator.getEmotes().get(GOTO_LAST)).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private final AtomicReference<Future<?>> timeout = new AtomicReference<>(null);
			private final Consumer<Void> success = s -> {
				handler.removeEvent(msg);
				if (timeout.get() != null) timeout.get().cancel(true);
				timeout.set(null);
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void accept(GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
						return;

					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (checkEmote(reaction, PREVIOUS)) {
						if (p > 0) {
							p--;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, NEXT)) {
						if (p < maxP) {
							p++;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, GOTO_FIRST)) {
						if (p > 0) {
							p = 0;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, GOTO_LAST)) {
						if (p < maxP) {
							p = maxP;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, CANCEL)) {
						try {
							if (msg.getChannel().getType().isGuild())
								msg.clearReactions().submit();
							else
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

							success.accept(null);
						} catch (InsufficientPermissionException | IllegalStateException e) {
							for (MessageReaction r : msg.getReactions()) {
								r.removeReaction().submit();
							}

							success.accept(null);
						}
					}

					if (timeout.get() != null) timeout.get().cancel(true);
					timeout.set(null);
					setTimeout(timeout, success, msg, time, unit);

					if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
						event.getReaction().removeReaction(u).submit();
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param fastForward Whether the {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST} buttons should be shown.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, boolean fastForward, @Nonnull Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		List<Page> pgs = Collections.unmodifiableList(pages);

		if (fastForward) msg.addReaction(paginator.getEmotes().get(GOTO_FIRST)).submit();
		msg.addReaction(paginator.getEmotes().get(PREVIOUS)).submit();
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		msg.addReaction(paginator.getEmotes().get(NEXT)).submit();
		if (fastForward) msg.addReaction(paginator.getEmotes().get(GOTO_LAST)).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private final Consumer<Void> success = s -> handler.removeEvent(msg);


			@Override
			public void accept(GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					if (canInteract.test(u)) {
						if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
							return;

						MessageReaction.ReactionEmote reaction = event.getReactionEmote();
						if (checkEmote(reaction, PREVIOUS)) {
							if (p > 0) {
								p--;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, NEXT)) {
							if (p < maxP) {
								p++;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, GOTO_FIRST)) {
							if (p > 0) {
								p = 0;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, GOTO_LAST)) {
							if (p < maxP) {
								p = maxP;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, CANCEL)) {
							try {
								if (msg.getChannel().getType().isGuild())
									msg.clearReactions().submit();
								else
									for (MessageReaction r : msg.getReactions()) {
										r.removeReaction().submit();
									}

								success.accept(null);
							} catch (InsufficientPermissionException | IllegalStateException e) {
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

								success.accept(null);
							}
						}

						if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
							event.getReaction().removeReaction(u).submit();
						}
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You can specify
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
	 * @param fastForward Whether the {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST} buttons should be shown.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int time, @Nonnull TimeUnit unit, boolean fastForward, @Nonnull Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		List<Page> pgs = Collections.unmodifiableList(pages);

		if (fastForward) msg.addReaction(paginator.getEmotes().get(GOTO_FIRST)).submit();
		msg.addReaction(paginator.getEmotes().get(PREVIOUS)).submit();
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		msg.addReaction(paginator.getEmotes().get(NEXT)).submit();
		if (fastForward) msg.addReaction(paginator.getEmotes().get(GOTO_LAST)).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private final AtomicReference<Future<?>> timeout = new AtomicReference<>(null);
			private final Consumer<Void> success = s -> {
				handler.removeEvent(msg);
				if (timeout.get() != null) timeout.get().cancel(true);
				timeout.set(null);
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void accept(GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					if (canInteract.test(u)) {
						if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
							return;

						MessageReaction.ReactionEmote reaction = event.getReactionEmote();
						if (checkEmote(reaction, PREVIOUS)) {
							if (p > 0) {
								p--;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, NEXT)) {
							if (p < maxP) {
								p++;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, GOTO_FIRST)) {
							if (p > 0) {
								p = 0;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, GOTO_LAST)) {
							if (p < maxP) {
								p = maxP;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, CANCEL)) {
							try {
								if (msg.getChannel().getType().isGuild())
									msg.clearReactions().submit();
								else
									for (MessageReaction r : msg.getReactions()) {
										r.removeReaction().submit();
									}

								success.accept(null);
							} catch (InsufficientPermissionException | IllegalStateException e) {
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

								success.accept(null);
							}
						}

						if (timeout.get() != null) timeout.get().cancel(true);
						timeout.set(null);
						setTimeout(timeout, success, msg, time, unit);

						if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
							event.getReaction().removeReaction(u).submit();
						}
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pages      The pages to be shown. The order of the {@link List} will
	 *                   define the order of the pages.
	 * @param skipAmount The amount of pages to be skipped when clicking {@link Emote#SKIP_BACKWARD}
	 *                   and {@link Emote#SKIP_FORWARD} buttons.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int skipAmount) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		List<Page> pgs = Collections.unmodifiableList(pages);

		if (skipAmount > 1) msg.addReaction(paginator.getEmotes().get(SKIP_BACKWARD)).submit();
		msg.addReaction(paginator.getEmotes().get(PREVIOUS)).submit();
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		msg.addReaction(paginator.getEmotes().get(NEXT)).submit();
		if (skipAmount > 1) msg.addReaction(paginator.getEmotes().get(SKIP_FORWARD)).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			@Override
			public void accept(GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
						return;

					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (checkEmote(reaction, PREVIOUS)) {
						if (p > 0) {
							p--;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, NEXT)) {
						if (p < maxP) {
							p++;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, SKIP_BACKWARD)) {
						if (p > 0) {
							p -= (p - skipAmount < 0 ? p : skipAmount);
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, SKIP_FORWARD)) {
						if (p < maxP) {
							p += (p + skipAmount > maxP ? maxP - p : skipAmount);
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, CANCEL)) {
						try {
							if (msg.getChannel().getType().isGuild())
								msg.clearReactions().submit();
							else
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

							success.accept(null);
						} catch (InsufficientPermissionException | IllegalStateException e) {
							for (MessageReaction r : msg.getReactions()) {
								r.removeReaction().submit();
							}

							success.accept(null);
						}
					}

					if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
						event.getReaction().removeReaction(u).submit();
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You can specify
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
	 * @param skipAmount The amount of pages to be skipped when clicking {@link Emote#SKIP_BACKWARD}
	 *                   and {@link Emote#SKIP_FORWARD} buttons.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int time, @Nonnull TimeUnit unit, int skipAmount) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		List<Page> pgs = Collections.unmodifiableList(pages);

		if (skipAmount > 1) msg.addReaction(paginator.getEmotes().get(SKIP_BACKWARD)).submit();
		msg.addReaction(paginator.getEmotes().get(PREVIOUS)).submit();
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		msg.addReaction(paginator.getEmotes().get(NEXT)).submit();
		if (skipAmount > 1) msg.addReaction(paginator.getEmotes().get(SKIP_FORWARD)).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private final AtomicReference<Future<?>> timeout = new AtomicReference<>(null);
			private final Consumer<Void> success = s -> {
				handler.removeEvent(msg);
				if (timeout.get() != null) timeout.get().cancel(true);
				timeout.set(null);
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void accept(GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
						return;

					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (checkEmote(reaction, PREVIOUS)) {
						if (p > 0) {
							p--;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, NEXT)) {
						if (p < maxP) {
							p++;
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, SKIP_BACKWARD)) {
						if (p > 0) {
							p -= (p - skipAmount < 0 ? p : skipAmount);
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, SKIP_FORWARD)) {
						if (p < maxP) {
							p += (p + skipAmount > maxP ? maxP - p : skipAmount);
							Page pg = pgs.get(p);

							updatePage(msg, pg);
						}
					} else if (checkEmote(reaction, CANCEL)) {
						try {
							if (msg.getChannel().getType().isGuild())
								msg.clearReactions().submit();
							else
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

							success.accept(null);
						} catch (InsufficientPermissionException | IllegalStateException e) {
							for (MessageReaction r : msg.getReactions()) {
								r.removeReaction().submit();
							}

							success.accept(null);
						}
					}

					if (timeout.get() != null) timeout.get().cancel(true);
					timeout.set(null);
					setTimeout(timeout, success, msg, time, unit);

					if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
						event.getReaction().removeReaction(u).submit();
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param skipAmount  The amount of pages to be skipped when clicking {@link Emote#SKIP_BACKWARD}
	 *                    and {@link Emote#SKIP_FORWARD} buttons.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int skipAmount, @Nonnull Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		List<Page> pgs = Collections.unmodifiableList(pages);

		if (skipAmount > 1) msg.addReaction(paginator.getEmotes().get(SKIP_BACKWARD)).submit();
		msg.addReaction(paginator.getEmotes().get(PREVIOUS)).submit();
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		msg.addReaction(paginator.getEmotes().get(NEXT)).submit();
		if (skipAmount > 1) msg.addReaction(paginator.getEmotes().get(SKIP_FORWARD)).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			@Override
			public void accept(GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					if (canInteract.test(u)) {
						if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
							return;

						MessageReaction.ReactionEmote reaction = event.getReactionEmote();
						if (checkEmote(reaction, PREVIOUS)) {
							if (p > 0) {
								p--;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, NEXT)) {
							if (p < maxP) {
								p++;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, SKIP_BACKWARD)) {
							if (p > 0) {
								p -= (p - skipAmount < 0 ? p : skipAmount);
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, SKIP_FORWARD)) {
							if (p < maxP) {
								p += (p + skipAmount > maxP ? maxP - p : skipAmount);
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, CANCEL)) {
							try {
								if (msg.getChannel().getType().isGuild())
									msg.clearReactions().submit();
								else
									for (MessageReaction r : msg.getReactions()) {
										r.removeReaction().submit();
									}

								success.accept(null);
							} catch (InsufficientPermissionException | IllegalStateException e) {
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

								success.accept(null);
							}
						}

						if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
							event.getReaction().removeReaction(u).submit();
						}
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You can specify
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
	 * @param skipAmount  The amount of pages to be skipped when clicking {@link Emote#SKIP_BACKWARD}
	 *                    and {@link Emote#SKIP_FORWARD} buttons.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int time, @Nonnull TimeUnit unit, int skipAmount, @Nonnull Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		List<Page> pgs = Collections.unmodifiableList(pages);

		if (skipAmount > 1) msg.addReaction(paginator.getEmotes().get(SKIP_BACKWARD)).submit();
		msg.addReaction(paginator.getEmotes().get(PREVIOUS)).submit();
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		msg.addReaction(paginator.getEmotes().get(NEXT)).submit();
		if (skipAmount > 1) msg.addReaction(paginator.getEmotes().get(SKIP_FORWARD)).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private final AtomicReference<Future<?>> timeout = new AtomicReference<>(null);
			private final Consumer<Void> success = s -> {
				handler.removeEvent(msg);
				if (timeout.get() != null) timeout.get().cancel(true);
				timeout.set(null);
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void accept(GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					if (canInteract.test(u)) {
						if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
							return;

						MessageReaction.ReactionEmote reaction = event.getReactionEmote();
						if (checkEmote(reaction, PREVIOUS)) {
							if (p > 0) {
								p--;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, NEXT)) {
							if (p < maxP) {
								p++;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, SKIP_BACKWARD)) {
							if (p > 0) {
								p -= (p - skipAmount < 0 ? p : skipAmount);
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, SKIP_FORWARD)) {
							if (p < maxP) {
								p += (p + skipAmount > maxP ? maxP - p : skipAmount);
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, CANCEL)) {
							try {
								if (msg.getChannel().getType().isGuild())
									msg.clearReactions().submit();
								else
									for (MessageReaction r : msg.getReactions()) {
										r.removeReaction().submit();
									}

								success.accept(null);
							} catch (InsufficientPermissionException | IllegalStateException e) {
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

								success.accept(null);
							}
						}

						if (timeout.get() != null) timeout.get().cancel(true);
						timeout.set(null);
						setTimeout(timeout, success, msg, time, unit);

						if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
							event.getReaction().removeReaction(u).submit();
						}
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param skipAmount  The amount of pages to be skipped when clicking {@link Emote#SKIP_BACKWARD}
	 *                    and {@link Emote#SKIP_FORWARD} buttons.
	 * @param fastForward Whether the {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST} buttons should be shown.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int skipAmount, boolean fastForward, @Nonnull Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		List<Page> pgs = Collections.unmodifiableList(pages);

		if (fastForward) msg.addReaction(paginator.getEmotes().get(GOTO_FIRST)).submit();
		if (skipAmount > 1) msg.addReaction(paginator.getEmotes().get(SKIP_BACKWARD)).submit();
		msg.addReaction(paginator.getEmotes().get(PREVIOUS)).submit();
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		msg.addReaction(paginator.getEmotes().get(NEXT)).submit();
		if (skipAmount > 1) msg.addReaction(paginator.getEmotes().get(SKIP_FORWARD)).submit();
		if (fastForward) msg.addReaction(paginator.getEmotes().get(GOTO_LAST)).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			@Override
			public void accept(GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					if (canInteract.test(u)) {
						if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
							return;

						MessageReaction.ReactionEmote reaction = event.getReactionEmote();
						if (checkEmote(reaction, PREVIOUS)) {
							if (p > 0) {
								p--;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, NEXT)) {
							if (p < maxP) {
								p++;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, SKIP_BACKWARD)) {
							if (p > 0) {
								p -= (p - skipAmount < 0 ? p : skipAmount);
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, SKIP_FORWARD)) {
							if (p < maxP) {
								p += (p + skipAmount > maxP ? maxP - p : skipAmount);
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, GOTO_FIRST)) {
							if (p > 0) {
								p = 0;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, GOTO_LAST)) {
							if (p < maxP) {
								p = maxP;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, CANCEL)) {
							try {
								if (msg.getChannel().getType().isGuild())
									msg.clearReactions().submit();
								else
									for (MessageReaction r : msg.getReactions()) {
										r.removeReaction().submit();
									}

								success.accept(null);
							} catch (InsufficientPermissionException | IllegalStateException e) {
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

								success.accept(null);
							}
						}

						if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
							event.getReaction().removeReaction(u).submit();
						}
					}
				});
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You can specify
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
	 * @param skipAmount  The amount of pages to be skipped when clicking {@link Emote#SKIP_BACKWARD}
	 *                    and {@link Emote#SKIP_FORWARD} buttons.
	 * @param fastForward Whether the {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST} buttons should be shown.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int time, @Nonnull TimeUnit unit, int skipAmount, boolean fastForward, @Nonnull Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		List<Page> pgs = Collections.unmodifiableList(pages);

		if (fastForward) msg.addReaction(paginator.getEmotes().get(GOTO_FIRST)).submit();
		if (skipAmount > 1) msg.addReaction(paginator.getEmotes().get(SKIP_BACKWARD)).submit();
		msg.addReaction(paginator.getEmotes().get(PREVIOUS)).submit();
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		msg.addReaction(paginator.getEmotes().get(NEXT)).submit();
		if (skipAmount > 1) msg.addReaction(paginator.getEmotes().get(SKIP_FORWARD)).submit();
		if (fastForward) msg.addReaction(paginator.getEmotes().get(GOTO_LAST)).submit();

		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private final AtomicReference<Future<?>> timeout = new AtomicReference<>(null);
			private final Consumer<Void> success = s -> {
				handler.removeEvent(msg);
				if (timeout.get() != null) timeout.get().cancel(true);
				timeout.set(null);
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void accept(GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					if (canInteract.test(u)) {
						if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
							return;

						MessageReaction.ReactionEmote reaction = event.getReactionEmote();
						if (checkEmote(reaction, PREVIOUS)) {
							if (p > 0) {
								p--;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, NEXT)) {
							if (p < maxP) {
								p++;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, SKIP_BACKWARD)) {
							if (p > 0) {
								p -= (p - skipAmount < 0 ? p : skipAmount);
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, SKIP_FORWARD)) {
							if (p < maxP) {
								p += (p + skipAmount > maxP ? maxP - p : skipAmount);
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, GOTO_FIRST)) {
							if (p > 0) {
								p = 0;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, GOTO_LAST)) {
							if (p < maxP) {
								p = maxP;
								Page pg = pgs.get(p);

								updatePage(msg, pg);
							}
						} else if (checkEmote(reaction, CANCEL)) {
							try {
								if (msg.getChannel().getType().isGuild())
									msg.clearReactions().submit();
								else
									for (MessageReaction r : msg.getReactions()) {
										r.removeReaction().submit();
									}

								success.accept(null);
							} catch (InsufficientPermissionException | IllegalStateException e) {
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

								success.accept(null);
							}
						}

						if (timeout.get() != null) timeout.get().cancel(true);
						timeout.set(null);
						setTimeout(timeout, success, msg, time, unit);

						if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
							event.getReaction().removeReaction(u).submit();
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
	 * will overwrite the current button's {@link Page}.
	 *
	 * @param msg        The {@link Message} sent which will be categorized.
	 * @param categories The categories to be shown. The categories are defined by
	 *                   a {@link Map} containing emoji unicodes or emote ids as keys and
	 *                   {@link Pages} as values.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 * @throws InvalidEmoteException           Thrown if one of the custom emotes' ID is invalid or not from a guild your bot is member of.
	 */
	public static void categorize(@Nonnull Message msg, @Nonnull Map<String, Page> categories) throws ErrorResponseException, InsufficientPermissionException, InvalidEmoteException {
		if (!isActivated()) throw new InvalidStateException();
		Map<String, Page> cats = Collections.unmodifiableMap(categories);

		for (String k : cats.keySet()) {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else {
				net.dv8tion.jda.api.entities.Emote e = getOrRetrieveEmote(k);
				if (e == null) throw new InvalidEmoteException();
				msg.addReaction(e).submit();
			}
		}
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private String currCat = "";
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			@Override
			public void accept(@Nonnull GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (u.isBot() || reaction.getName().equals(currCat) || msg == null || !event.getMessageId().equals(msg.getId()))
						return;

					if (checkEmote(reaction, CANCEL)) {
						try {
							if (msg.getChannel().getType().isGuild())
								msg.clearReactions().submit();
							else
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

							success.accept(null);
						} catch (InsufficientPermissionException | IllegalStateException e) {
							for (MessageReaction r : msg.getReactions()) {
								r.removeReaction().submit();
							}

							success.accept(null);
						}
						return;
					}

					Page pg = cats.get(reaction.isEmoji() ? reaction.getName() : reaction.getId());

					currCat = updateCategory(reaction.getName(), msg, pg);
					if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
						event.getReaction().removeReaction(u).submit();
					}
				});
			}
		});
	}

	/**
	 * Adds menu-like buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will browse through a given {@link Map} of pages. You may only specify
	 * one {@link Page} per button, adding another button with an existing unicode
	 * will overwrite the current button's {@link Page}. You can specify how long
	 * the listener will stay active before shutting down itself after a no-activity
	 * interval.
	 *
	 * @param msg        The {@link Message} sent which will be categorized.
	 * @param categories The categories to be shown. The categories are defined by a
	 *                   {@link Map} containing emoji unicodes or emote ids as keys and
	 *                   {@link Pages} as values.
	 * @param time       The time before the listener automatically stop listening
	 *                   for further events (recommended: 60).
	 * @param unit       The time's {@link TimeUnit} (recommended:
	 *                   {@link TimeUnit#SECONDS}).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 * @throws InvalidEmoteException           Thrown if one of the custom emotes' ID is invalid or not from a guild your bot is member of.
	 */
	public static void categorize(@Nonnull Message msg, @Nonnull Map<String, Page> categories, int time, @Nonnull TimeUnit unit) throws ErrorResponseException, InsufficientPermissionException, InvalidEmoteException {
		if (!isActivated()) throw new InvalidStateException();
		Map<String, Page> cats = Collections.unmodifiableMap(categories);

		for (String k : cats.keySet()) {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else {
				net.dv8tion.jda.api.entities.Emote e = getOrRetrieveEmote(k);
				if (e == null) throw new InvalidEmoteException();
				msg.addReaction(e).submit();
			}
		}
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private String currCat = "";
			private final AtomicReference<Future<?>> timeout = new AtomicReference<>(null);
			private final Consumer<Void> success = s -> {
				handler.removeEvent(msg);
				if (timeout.get() != null) timeout.get().cancel(true);
				timeout.set(null);
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void accept(@Nonnull GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (u.isBot() || reaction.getName().equals(currCat) || msg == null || !event.getMessageId().equals(msg.getId()))
						return;

					if (checkEmote(reaction, CANCEL)) {
						try {
							if (msg.getChannel().getType().isGuild())
								msg.clearReactions().submit();
							else
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

							success.accept(null);
						} catch (InsufficientPermissionException | IllegalStateException e) {
							for (MessageReaction r : msg.getReactions()) {
								r.removeReaction().submit();
							}

							success.accept(null);
						}
						return;
					}

					if (timeout.get() != null) timeout.get().cancel(true);
					timeout.set(null);
					setTimeout(timeout, success, msg, time, unit);

					Page pg = cats.get(reaction.isEmoji() ? reaction.getName() : reaction.getId());

					currCat = updateCategory(reaction.getName(), msg, pg);
					if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
						event.getReaction().removeReaction(u).submit();
					}
				});
			}
		});
	}

	/**
	 * Adds menu-like buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will browse through a given {@link Map} of pages. You may only specify
	 * one {@link Page} per button, adding another button with an existing unicode
	 * will overwrite the current button's {@link Page}.
	 *
	 * @param msg         The {@link Message} sent which will be categorized.
	 * @param categories  The categories to be shown. The categories are defined by
	 *                    a {@link Map} containing emoji unicodes or emote ids as keys and
	 *                    {@link Pages} as values.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 * @throws InvalidEmoteException           Thrown if one of the custom emotes' ID is invalid or not from a guild your bot is member of.
	 */
	public static void categorize(@Nonnull Message msg, @Nonnull Map<String, Page> categories, @Nonnull Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException, InvalidEmoteException {
		if (!isActivated()) throw new InvalidStateException();
		Map<String, Page> cats = Collections.unmodifiableMap(categories);

		for (String k : cats.keySet()) {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else {
				net.dv8tion.jda.api.entities.Emote e = getOrRetrieveEmote(k);
				if (e == null) throw new InvalidEmoteException();
				msg.addReaction(e).submit();
			}
		}
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private String currCat = "";
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			@Override
			public void accept(@Nonnull GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (canInteract.test(u)) {
						if (u.isBot() || reaction.getName().equals(currCat) || msg == null || !event.getMessageId().equals(msg.getId()))
							return;

						if (checkEmote(reaction, CANCEL)) {
							try {
								if (msg.getChannel().getType().isGuild())
									msg.clearReactions().submit();
								else
									for (MessageReaction r : msg.getReactions()) {
										r.removeReaction().submit();
									}

								success.accept(null);
							} catch (InsufficientPermissionException | IllegalStateException e) {
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

								success.accept(null);
							}
							return;
						}

						Page pg = cats.get(reaction.isEmoji() ? reaction.getName() : reaction.getId());

						currCat = updateCategory(reaction.getName(), msg, pg);
						if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
							event.getReaction().removeReaction(u).submit();
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
	 * will overwrite the current button's {@link Page}. You can specify how long
	 * the listener will stay active before shutting down itself after a no-activity
	 * interval.
	 *
	 * @param msg         The {@link Message} sent which will be categorized.
	 * @param categories  The categories to be shown. The categories are defined by
	 *                    a {@link Map} containing emoji unicodes or emote ids as keys and
	 *                    {@link Pages} as values.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 * @throws InvalidEmoteException           Thrown if one of the custom emotes' ID is invalid or not from a guild your bot is member of.
	 */
	public static void categorize(@Nonnull Message msg, @Nonnull Map<String, Page> categories, int time, @Nonnull TimeUnit unit, @Nonnull Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException, InvalidEmoteException {
		if (!isActivated()) throw new InvalidStateException();
		Map<String, Page> cats = Collections.unmodifiableMap(categories);

		for (String k : cats.keySet()) {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else {
				net.dv8tion.jda.api.entities.Emote e = getOrRetrieveEmote(k);
				if (e == null) throw new InvalidEmoteException();
				msg.addReaction(e).submit();
			}
		}
		msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private String currCat = "";
			private final AtomicReference<Future<?>> timeout = new AtomicReference<>(null);
			private final Consumer<Void> success = s -> {
				handler.removeEvent(msg);
				if (timeout.get() != null) timeout.get().cancel(true);
				timeout.set(null);
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void accept(@Nonnull GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (canInteract.test(u)) {
						if (u.isBot() || reaction.getName().equals(currCat) || msg == null || !event.getMessageId().equals(msg.getId()))
							return;

						if (checkEmote(reaction, CANCEL)) {
							try {
								if (msg.getChannel().getType().isGuild())
									msg.clearReactions().submit();
								else
									for (MessageReaction r : msg.getReactions()) {
										r.removeReaction().submit();
									}

								success.accept(null);
							} catch (InsufficientPermissionException | IllegalStateException e) {
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

								success.accept(null);
							}
							return;
						}

						Page pg = cats.get(reaction.isEmoji() ? reaction.getName() : reaction.getId());

						if (timeout.get() != null) timeout.get().cancel(true);
						timeout.set(null);
						setTimeout(timeout, success, msg, time, unit);

						currCat = updateCategory(reaction.getName(), msg, pg);
						if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
							event.getReaction().removeReaction(u).submit();
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
	 *                         {@link Map} containing emoji unicodes or emote ids as keys and
	 *                         {@link ThrowingBiConsumer}&lt;{@link Member}, {@link Message}&gt;.
	 *                         containing desired behavior as value.
	 * @param showCancelButton Should the {@link Emote#CANCEL} button be created automatically?
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 * @throws InvalidEmoteException           Thrown if one of the custom emotes' ID is invalid or not from a guild your bot is member of.
	 */
	public static void buttonize(@Nonnull Message msg, @Nonnull Map<String, ThrowingBiConsumer<Member, Message>> buttons, boolean showCancelButton) throws ErrorResponseException, InsufficientPermissionException, InvalidEmoteException {
		if (!isActivated()) throw new InvalidStateException();
		Map<String, ThrowingBiConsumer<Member, Message>> btns = Collections.unmodifiableMap(buttons);

		for (String k : btns.keySet()) {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else {
				net.dv8tion.jda.api.entities.Emote e = getOrRetrieveEmote(k);
				if (e == null) throw new InvalidEmoteException();
				msg.addReaction(e).submit();
			}
		}
		if (!btns.containsKey(paginator.getEmote(CANCEL)) && showCancelButton)
			msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			@Override
			public void accept(@Nonnull GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
						return;

					if ((!btns.containsKey(paginator.getEmote(CANCEL)) && showCancelButton) && checkEmote(reaction, CANCEL)) {
						try {
							if (msg.getChannel().getType().isGuild())
								msg.clearReactions().submit();
							else
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

							success.accept(null);
						} catch (InsufficientPermissionException | IllegalStateException e) {
							for (MessageReaction r : msg.getReactions()) {
								r.removeReaction().submit();
							}

							success.accept(null);
						}
						return;
					}

					if (reaction.isEmoji())
						btns.get(reaction.getName()).accept(event.getMember(), msg);
					else
						btns.get(reaction.getId()).accept(event.getMember(), msg);

					if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
						event.getReaction().removeReaction(u).submit();
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
	 *                         Map containing emoji unicodes or emote ids as keys and
	 *                         {@link ThrowingBiConsumer}&lt;{@link Member}, {@link Message}&gt;
	 *                         containing desired behavior as value.
	 * @param showCancelButton Should the {@link Emote#CANCEL} button be created automatically?
	 * @param time             The time before the listener automatically stop
	 *                         listening for further events (recommended: 60).
	 * @param unit             The time's {@link TimeUnit} (recommended:
	 *                         {@link TimeUnit#SECONDS}).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 * @throws InvalidEmoteException           Thrown if one of the custom emotes' ID is invalid or not from a guild your bot is member of.
	 */
	public static void buttonize(@Nonnull Message msg, @Nonnull Map<String, ThrowingBiConsumer<Member, Message>> buttons, boolean showCancelButton, int time, @Nonnull TimeUnit unit) throws ErrorResponseException, InsufficientPermissionException, InvalidEmoteException {
		if (!isActivated()) throw new InvalidStateException();
		Map<String, ThrowingBiConsumer<Member, Message>> btns = Collections.unmodifiableMap(buttons);

		for (String k : btns.keySet()) {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else {
				net.dv8tion.jda.api.entities.Emote e = getOrRetrieveEmote(k);
				if (e == null) throw new InvalidEmoteException();
				msg.addReaction(e).submit();
			}
		}
		if (!btns.containsKey(paginator.getEmote(CANCEL)) && showCancelButton)
			msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final AtomicReference<Future<?>> timeout = new AtomicReference<>(null);
			private final Consumer<Void> success = s -> {
				handler.removeEvent(msg);
				if (timeout.get() != null) timeout.get().cancel(true);
				timeout.set(null);
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void accept(@Nonnull GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
						return;

					if ((!btns.containsKey(paginator.getEmote(CANCEL)) && showCancelButton) && checkEmote(reaction, CANCEL)) {
						try {
							if (msg.getChannel().getType().isGuild())
								msg.clearReactions().submit();
							else
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

							success.accept(null);
						} catch (InsufficientPermissionException | IllegalStateException e) {
							for (MessageReaction r : msg.getReactions()) {
								r.removeReaction().submit();
							}

							success.accept(null);
						}
						return;
					}

					if (reaction.isEmoji())
						btns.get(reaction.getName()).accept(event.getMember(), msg);
					else
						btns.get(reaction.getId()).accept(event.getMember(), msg);

					if (timeout.get() != null) timeout.get().cancel(true);
					timeout.set(null);
					setTimeout(timeout, success, msg, time, unit);

					if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
						event.getReaction().removeReaction(u).submit();
					}
				});
			}
		});
	}

	/**
	 * Adds buttons to the specified {@link Message}/{@link MessageEmbed}, with each
	 * executing a specific task on click. You must only specify one
	 * {@link Runnable} per button, adding another button with an existing unicode
	 * will overwrite the current button's {@link Runnable}.
	 *
	 * @param msg              The {@link Message} sent which will be buttoned.
	 * @param buttons          The buttons to be shown. The buttons are defined by a
	 *                         Map containing emoji unicodes or emote ids as keys and
	 *                         {@link ThrowingBiConsumer}&lt;{@link Member}, {@link Message}&gt;
	 *                         containing desired behavior as value.
	 * @param showCancelButton Should the {@link Emote#CANCEL} button be created automatically?
	 * @param canInteract      {@link Predicate} to determine whether the
	 *                         {@link User} that pressed the button can interact
	 *                         with it or not.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 * @throws InvalidEmoteException           Thrown if one of the custom emotes' ID is invalid or not from a guild your bot is member of.
	 */
	public static void buttonize(@Nonnull Message msg, @Nonnull Map<String, ThrowingBiConsumer<Member, Message>> buttons, boolean showCancelButton, @Nonnull Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException, InvalidEmoteException {
		if (!isActivated()) throw new InvalidStateException();
		Map<String, ThrowingBiConsumer<Member, Message>> btns = Collections.unmodifiableMap(buttons);

		for (String k : btns.keySet()) {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else {
				net.dv8tion.jda.api.entities.Emote e = getOrRetrieveEmote(k);
				if (e == null) throw new InvalidEmoteException();
				msg.addReaction(e).submit();
			}
		}
		if (!btns.containsKey(paginator.getEmote(CANCEL)) && showCancelButton)
			msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final Consumer<Void> success = s -> handler.removeEvent(msg);

			@Override
			public void accept(@Nonnull GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (canInteract.test(u)) {
						if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
							return;

						if ((!btns.containsKey(paginator.getEmote(CANCEL)) && showCancelButton) && checkEmote(reaction, CANCEL)) {
							try {
								if (msg.getChannel().getType().isGuild())
									msg.clearReactions().submit();
								else
									for (MessageReaction r : msg.getReactions()) {
										r.removeReaction().submit();
									}

								success.accept(null);
							} catch (InsufficientPermissionException | IllegalStateException e) {
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

								success.accept(null);
							}
							return;
						}

						if (reaction.isEmoji())
							btns.get(reaction.getName()).accept(event.getMember(), msg);
						else
							btns.get(reaction.getId()).accept(event.getMember(), msg);

						if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
							event.getReaction().removeReaction(u).submit();
						}
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
	 *                         Map containing emoji unicodes or emote ids as keys and
	 *                         {@link ThrowingBiConsumer}&lt;{@link Member}, {@link Message}&gt;
	 *                         containing desired behavior as value.
	 * @param showCancelButton Should the {@link Emote#CANCEL} button be created automatically?
	 * @param time             The time before the listener automatically stop
	 *                         listening for further events (recommended: 60).
	 * @param unit             The time's {@link TimeUnit} (recommended:
	 *                         {@link TimeUnit#SECONDS}).
	 * @param canInteract      {@link Predicate} to determine whether the
	 *                         {@link User} that pressed the button can interact
	 *                         with it or not.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 * @throws InvalidEmoteException           Thrown if one of the custom emotes' ID is invalid or not from a guild your bot is member of.
	 */
	public static void buttonize(@Nonnull Message msg, @Nonnull Map<String, ThrowingBiConsumer<Member, Message>> buttons, boolean showCancelButton, int time, @Nonnull TimeUnit unit, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException, InvalidEmoteException {
		if (!isActivated()) throw new InvalidStateException();
		Map<String, ThrowingBiConsumer<Member, Message>> btns = Collections.unmodifiableMap(buttons);

		for (String k : btns.keySet()) {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else {
				net.dv8tion.jda.api.entities.Emote e = getOrRetrieveEmote(k);
				if (e == null) throw new InvalidEmoteException();
				msg.addReaction(e).submit();
			}
		}
		if (!btns.containsKey(paginator.getEmote(CANCEL)) && showCancelButton)
			msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final AtomicReference<Future<?>> timeout = new AtomicReference<>(null);
			private final Consumer<Void> success = s -> {
				handler.removeEvent(msg);
				if (timeout.get() != null) timeout.get().cancel(true);
				timeout.set(null);
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void accept(@Nonnull GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (canInteract.test(u)) {
						if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
							return;

						if ((!btns.containsKey(paginator.getEmote(CANCEL)) && showCancelButton) && checkEmote(reaction, CANCEL)) {
							try {
								if (msg.getChannel().getType().isGuild())
									msg.clearReactions().submit();
								else
									for (MessageReaction r : msg.getReactions()) {
										r.removeReaction().submit();
									}

								success.accept(null);
							} catch (InsufficientPermissionException | IllegalStateException e) {
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

								success.accept(null);
							}
							return;
						}

						if (reaction.isEmoji())
							btns.get(reaction.getName()).accept(event.getMember(), msg);
						else
							btns.get(reaction.getId()).accept(event.getMember(), msg);

						if (timeout.get() != null) timeout.get().cancel(true);
						timeout.set(null);
						setTimeout(timeout, success, msg, time, unit);

						if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
							event.getReaction().removeReaction(u).submit();
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
	 *                         Map containing emoji unicodes or emote ids as keys and
	 *                         {@link ThrowingBiConsumer}&lt;{@link Member}, {@link Message}&gt;
	 *                         containing desired behavior as value.
	 * @param showCancelButton Should the {@link Emote#CANCEL} button be created automatically?
	 * @param canInteract      {@link Predicate} to determine whether the
	 *                         {@link User} that pressed the button can interact
	 *                         with it or not.
	 * @param onCancel         Action to be ran after the listener is removed.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void buttonize(@Nonnull Message msg, @Nonnull Map<String, ThrowingBiConsumer<Member, Message>> buttons, boolean showCancelButton, @Nonnull Predicate<User> canInteract, @Nonnull Consumer<Message> onCancel) throws ErrorResponseException, InsufficientPermissionException, InvalidEmoteException {
		if (!isActivated()) throw new InvalidStateException();
		Map<String, ThrowingBiConsumer<Member, Message>> btns = Collections.unmodifiableMap(buttons);

		for (String k : btns.keySet()) {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else {
				net.dv8tion.jda.api.entities.Emote e = getOrRetrieveEmote(k);
				if (e == null) throw new InvalidEmoteException();
				msg.addReaction(e).submit();
			}
		}
		if (!btns.containsKey(paginator.getEmote(CANCEL)) && showCancelButton)
			msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final Consumer<Void> success = s -> {
				handler.removeEvent(msg);
				onCancel.accept(msg);
			};

			@Override
			public void accept(@Nonnull GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (canInteract.test(u)) {
						if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
							return;

						if ((!btns.containsKey(paginator.getEmote(CANCEL)) && showCancelButton) && checkEmote(reaction, CANCEL)) {
							try {
								if (msg.getChannel().getType().isGuild())
									msg.clearReactions().submit();
								else
									for (MessageReaction r : msg.getReactions()) {
										r.removeReaction().submit();
									}

								success.accept(null);
							} catch (InsufficientPermissionException | IllegalStateException e) {
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

								success.accept(null);
							}
							return;
						}

						if (reaction.isEmoji())
							btns.get(reaction.getName()).accept(event.getMember(), msg);
						else
							btns.get(reaction.getId()).accept(event.getMember(), msg);

						if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
							event.getReaction().removeReaction(u).submit();
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
	 *                         Map containing emoji unicodes or emote ids as keys and
	 *                         {@link ThrowingBiConsumer}&lt;{@link Member}, {@link Message}&gt;
	 *                         containing desired behavior as value.
	 * @param showCancelButton Should the {@link Emote#CANCEL} button be created automatically?
	 * @param time             The time before the listener automatically stop
	 *                         listening for further events (recommended: 60).
	 * @param unit             The time's {@link TimeUnit} (recommended:
	 *                         {@link TimeUnit#SECONDS}).
	 * @param canInteract      {@link Predicate} to determine whether the
	 *                         {@link User} that pressed the button can interact
	 *                         with it or not.
	 * @param onCancel         Action to be ran after the listener is removed.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 * @throws InvalidEmoteException           Thrown if one of the custom emotes' ID is invalid or not from a guild your bot is member of.
	 */
	public static void buttonize(@Nonnull Message msg, @Nonnull Map<String, ThrowingBiConsumer<Member, Message>> buttons, boolean showCancelButton, int time, @Nonnull TimeUnit unit, @Nonnull Predicate<User> canInteract, @Nonnull Consumer<Message> onCancel) throws ErrorResponseException, InsufficientPermissionException, InvalidEmoteException {
		if (!isActivated()) throw new InvalidStateException();
		Map<String, ThrowingBiConsumer<Member, Message>> btns = Collections.unmodifiableMap(buttons);

		for (String k : btns.keySet()) {
			if (EmojiUtils.containsEmoji(k))
				msg.addReaction(k).submit();
			else {
				net.dv8tion.jda.api.entities.Emote e = getOrRetrieveEmote(k);
				if (e == null) throw new InvalidEmoteException();
				msg.addReaction(e).submit();
			}
		}
		if (!btns.containsKey(paginator.getEmote(CANCEL)) && showCancelButton)
			msg.addReaction(paginator.getEmotes().get(CANCEL)).submit();
		handler.addEvent((msg.getChannelType().isGuild() ? msg.getGuild().getId() : msg.getPrivateChannel().getId()) + msg.getId(), new Consumer<GenericMessageReactionEvent>() {
			private final AtomicReference<Future<?>> timeout = new AtomicReference<>(null);
			private final Consumer<Void> success = s -> {
				handler.removeEvent(msg);
				onCancel.accept(msg);
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void accept(@Nonnull GenericMessageReactionEvent event) {
				event.retrieveUser().submit().thenAccept(u -> {
					Message msg = null;
					try {
						msg = event.retrieveMessage().submit().get();
					} catch (InterruptedException | ExecutionException ignore) {
					}
					MessageReaction.ReactionEmote reaction = event.getReactionEmote();
					if (canInteract.test(u)) {
						if (u.isBot() || msg == null || !event.getMessageId().equals(msg.getId()))
							return;

						if ((!btns.containsKey(paginator.getEmote(CANCEL)) && showCancelButton) && checkEmote(reaction, CANCEL)) {
							try {
								if (msg.getChannel().getType().isGuild())
									msg.clearReactions().submit();
								else
									for (MessageReaction r : msg.getReactions()) {
										r.removeReaction().submit();
									}

								success.accept(null);
							} catch (InsufficientPermissionException | IllegalStateException e) {
								for (MessageReaction r : msg.getReactions()) {
									r.removeReaction().submit();
								}

								success.accept(null);
							}
							return;
						}

						if (reaction.isEmoji())
							btns.get(reaction.getName()).accept(event.getMember(), msg);
						else
							btns.get(reaction.getId()).accept(event.getMember(), msg);

						if (timeout.get() != null) timeout.get().cancel(true);
						timeout.set(null);
						setTimeout(timeout, success, msg, time, unit);

						if (event.isFromGuild() && (paginator == null || paginator.isRemoveOnReact())) {
							event.getReaction().removeReaction(u).submit();
						}
					}
				});
			}
		});
	}

	/**
	 * Method used to update the current page.
	 * <strong>Must not be called outside of {@link Pages}&lt;</strong>.
	 *
	 * @param msg The current {@link Message} object.
	 * @param p   The current {@link Page} index.
	 */
	private static void updatePage(@Nonnull Message msg, Page p) {
		if (p == null) throw new NullPageException();
		if (p.getType() == PageType.TEXT) {
			msg.editMessage((Message) p.getContent()).submit();
		} else {
			msg.editMessage((MessageEmbed) p.getContent()).submit();
		}
	}

	/**
	 * Method used to update the current category.
	 * <strong>Must not be called outside of {@link Pages}&lt;</strong>.
	 *
	 * @param emote The button pressed by the user.
	 * @param msg   The current {@link Message} object.
	 * @param p     The current {@link Page} category.
	 * @return The new {@link Page} category.
	 */
	private static String updateCategory(String emote, @Nonnull Message msg, Page p) {
		AtomicReference<String> out = new AtomicReference<>("");
		if (p == null) throw new NullPageException();

		if (p.getType() == PageType.TEXT) {
			msg.editMessage((Message) p.getContent()).submit()
					.thenAccept(s -> out.set(emote));
		} else {
			msg.editMessage((MessageEmbed) p.getContent()).submit()
					.thenAccept(s -> out.set(emote));
		}

		return out.get();
	}

	/**
	 * Method used to set expiration of the events.
	 * <strong>Must not be called outside of {@link Pages}&lt;</strong>.
	 *
	 * @param timeout The {@link Future} reference which will contain expiration action.
	 * @param success The {@link Consumer} to be called after expiration.
	 * @param msg     The {@link Message} related to this event.
	 * @param time    How much time before expiration.
	 * @param unit    The {@link TimeUnit} for the expiration time.
	 */
	private static void setTimeout(AtomicReference<Future<?>> timeout, Consumer<Void> success, Message msg, int time, TimeUnit unit) {
		try {
			timeout.set(msg.clearReactions().submitAfter(time, unit).thenAccept(success));
		} catch (InsufficientPermissionException | IllegalStateException e) {
			timeout.set(msg.getChannel()
					.retrieveMessageById(msg.getId())
					.submitAfter(time, unit)
					.thenCompose(message -> {
						CompletableFuture<?>[] removeReaction = new CompletableFuture[message.getReactions().size()];

						for (int i = 0; i < message.getReactions().size(); i++) {
							MessageReaction r = message.getReactions().get(i);

							if (!r.isSelf()) continue;

							removeReaction[i] = r.removeReaction().submit();
						}

						return CompletableFuture.allOf(removeReaction).thenAccept(success);
					})
			);
		}
	}

	/**
	 * Utility method used to check if a reaction's {@link net.dv8tion.jda.api.entities.Emote} equals
	 * to given {@link Emote} set during building.
	 * <strong>Must not be called outside of {@link Pages}&lt;</strong>.
	 *
	 * @param reaction The reaction returned by {@link ListenerAdapter#onMessageReactionAdd}.
	 * @param emote    The {@link Emote} to check.
	 * @return Whether the reaction's name or ID equals to the {@link Emote}'s definition.
	 */
	private static boolean checkEmote(MessageReaction.ReactionEmote reaction, Emote emote) {
		if (reaction.isEmoji() && reaction.getName().equals(paginator.getEmote(emote)))
			return true;
		else return reaction.isEmote() && reaction.getId().equals(paginator.getEmote(emote));
	}

	/**
	 * Utility method to either retrieve the Emote by using a {@link RestAction} or get from
	 * the cache.
	 * <strong>Must not be called outside of {@link Pages}&lt;</strong>.
	 *
	 * @param id The {@link net.dv8tion.jda.api.entities.Emote}'s ID.
	 * @return The {@link net.dv8tion.jda.api.entities.Emote} object if found, else returns null.
	 */
	private static net.dv8tion.jda.api.entities.Emote getOrRetrieveEmote(String id) {
		net.dv8tion.jda.api.entities.Emote e = null;
		if (paginator.getHandler() instanceof JDA) {
			JDA handler = (JDA) paginator.getHandler();

			if (handler.getEmotes().isEmpty()) {
				Guild g = handler.getGuildById(paginator.getEmoteMap().getOrDefault(id, "0"));

				if (g != null) {
					e = g.retrieveEmoteById(id).complete();
				} else for (Guild guild : handler.getGuilds()) {
					try {
						e = guild.retrieveEmoteById(id).complete();
						break;
					} catch (ErrorResponseException ignore) {
					}
				}

				if (e != null && e.getGuild() != null)
					paginator.getEmoteMap().put(id, e.getGuild().getId());
			} else e = handler.getEmoteById(id);
		} else if (paginator.getHandler() instanceof ShardManager) {
			ShardManager handler = (ShardManager) paginator.getHandler();

			if (handler.getEmotes().isEmpty()) {
				Guild g = handler.getGuildById(paginator.getEmoteMap().getOrDefault(id, "0"));

				if (g != null) {
					e = g.retrieveEmoteById(id).complete();
				} else for (Guild guild : handler.getGuilds()) {
					try {
						e = guild.retrieveEmoteById(id).complete();
						break;
					} catch (ErrorResponseException ignore) {
					}
				}

				if (e != null && e.getGuild() != null)
					paginator.getEmoteMap().put(id, e.getGuild().getId());
			} else e = handler.getEmoteById(id);
		}

		return e;
	}
}
