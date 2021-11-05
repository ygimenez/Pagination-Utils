package com.github.ygimenez.method;

import com.github.ygimenez.exception.AlreadyActivatedException;
import com.github.ygimenez.exception.InvalidHandlerException;
import com.github.ygimenez.exception.InvalidStateException;
import com.github.ygimenez.exception.NullPageException;
import com.github.ygimenez.listener.MessageHandler;
import com.github.ygimenez.model.*;
import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.ygimenez.type.Emote.*;

/**
 * The main class containing all pagination-related methods, including but not limited
 * to {@link #paginate(Message, List)}, {@link #categorize(Message, Map, boolean)},
 * {@link #buttonize(Message, Map, boolean, boolean)} and {@link #lazyPaginate(Message, ThrowingFunction)}.
 */
public class Pages {
	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private static final MessageHandler handler = new MessageHandler();
	private static Paginator paginator;

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
		paginator.log(PUtilsConfig.LogLevel.LEVEL_2, "Pagination Utils activated successfully");
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

		paginator.log(PUtilsConfig.LogLevel.LEVEL_2, "Pagination Utils deactivated successfully");
		paginator = null;
	}

	/**
	 * Checks whether this library has been activated or not.
	 *
	 * @return The activation state of this library.
	 */
	public static boolean isActivated() {
		return paginator != null && paginator.getHandler() != null;
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
	 * @param pages The pages to be shown. The order of the {@link List} will
	 *              define the order of the pages.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages) throws ErrorResponseException, InsufficientPermissionException {
		paginate(msg, pages, 0, null, 0, false, null);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You can specify
	 * how long the listener will stay active before shutting down itself after a
	 * no-activity interval.
	 *
	 * @param msg   The {@link Message} sent which will be paginated.
	 * @param pages The pages to be shown. The order of the {@link List} will
	 *              define the order of the pages.
	 * @param time  The time before the listener automatically stop listening
	 *              for further events (recommended: 60).
	 * @param unit  The time's {@link TimeUnit} (recommended:
	 *              {@link TimeUnit#SECONDS}).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int time, TimeUnit unit) throws ErrorResponseException, InsufficientPermissionException {
		paginate(msg, pages, time, unit, 0, false, null);
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
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		paginate(msg, pages, 0, null, 0, false, canInteract);
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
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int time, TimeUnit unit, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		paginate(msg, pages, time, unit, 0, false, canInteract);
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
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, boolean fastForward) throws ErrorResponseException, InsufficientPermissionException {
		paginate(msg, pages, 0, null, 0, fastForward, null);
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
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int time, TimeUnit unit, boolean fastForward) throws ErrorResponseException, InsufficientPermissionException {
		paginate(msg, pages, time, unit, 0, fastForward, null);
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
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, boolean fastForward, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		paginate(msg, pages, 0, null, 0, fastForward, canInteract);
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
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int time, TimeUnit unit, boolean fastForward, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		paginate(msg, pages, time, unit, 0, fastForward, canInteract);
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
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int skipAmount) throws ErrorResponseException, InsufficientPermissionException {
		paginate(msg, pages, 0, null, skipAmount, false, null);
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
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int time, TimeUnit unit, int skipAmount) throws ErrorResponseException, InsufficientPermissionException {
		paginate(msg, pages, time, unit, skipAmount, false, null);
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
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int skipAmount, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		paginate(msg, pages, 0, null, skipAmount, false, canInteract);
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
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int time, TimeUnit unit, int skipAmount, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		paginate(msg, pages, time, unit, skipAmount, false, canInteract);
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
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int skipAmount, boolean fastForward, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		paginate(msg, pages, 0, null, skipAmount, fastForward, canInteract);
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
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int time, TimeUnit unit, int skipAmount, boolean fastForward) throws ErrorResponseException, InsufficientPermissionException {
		paginate(msg, pages, time, unit, skipAmount, fastForward, null);
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
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static void paginate(@Nonnull Message msg, @Nonnull List<Page> pages, int time, TimeUnit unit, int skipAmount, boolean fastForward, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated() || pages.isEmpty()) throw new InvalidStateException();
		List<Page> pgs = Collections.unmodifiableList(pages);
		clearButtons(msg);
		clearReactions(msg);

		Page pg = pgs.get(0);
		if (pg instanceof InteractPage) addButtons((InteractPage) pg, msg, skipAmount > 1, fastForward);
		else addReactions(msg, skipAmount > 1, fastForward);

		handler.addEvent(msg, new ThrowingBiConsumer<>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private final AtomicReference<ScheduledFuture<?>> timeout = new AtomicReference<>(null);
			private final Consumer<Void> success = s -> {
				if (timeout.get() != null)
					timeout.get().cancel(true);
				handler.removeEvent(msg);
				if (paginator.isDeleteOnCancel()) msg.delete().submit();
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void acceptThrows(@Nonnull User u, @Nonnull PaginationEventWrapper wrapper) {
				Message m = subGet(wrapper.retrieveMessage());

				if (canInteract == null || canInteract.test(u)) {
					if (u.isBot() || m == null || !wrapper.getMessageId().equals(msg.getId()))
						return;

					Emote emt = NONE;
					if (wrapper.getContent() instanceof MessageReaction) {
						MessageReaction.ReactionEmote reaction = ((MessageReaction) wrapper.getContent()).getReactionEmote();
						emt = toEmote(reaction);
					} else if (wrapper.getContent() instanceof Button) {
						String id = ((Button) wrapper.getContent()).getId();
						if (id != null && !id.contains(".")) {
							emt = Emote.valueOf(id.replace("*", ""));
						}
					}

					switch (emt) {
						case PREVIOUS:
							if (p > 0) {
								p--;
								Page pg = pgs.get(p);

								updatePage(m, pg);
								updateButtons(m, pg, skipAmount > 1, fastForward);
							}
							break;
						case NEXT:
							if (p < maxP) {
								p++;
								Page pg = pgs.get(p);

								updatePage(m, pg);
								updateButtons(m, pg, skipAmount > 1, fastForward);
							}
							break;
						case SKIP_BACKWARD:
							if (p > 0) {
								p -= (p - skipAmount < 0 ? p : skipAmount);
								Page pg = pgs.get(p);

								updatePage(m, pg);
								updateButtons(m, pg, skipAmount > 1, fastForward);
							}
							break;
						case SKIP_FORWARD:
							if (p < maxP) {
								p += (p + skipAmount > maxP ? maxP - p : skipAmount);
								Page pg = pgs.get(p);

								updatePage(m, pg);
								updateButtons(m, pg, skipAmount > 1, fastForward);
							}
							break;
						case GOTO_FIRST:
							if (p > 0) {
								p = 0;
								Page pg = pgs.get(p);

								updatePage(m, pg);
								updateButtons(m, pg, skipAmount > 1, fastForward);
							}
							break;
						case GOTO_LAST:
							if (p < maxP) {
								p = maxP;
								Page pg = pgs.get(p);

								updatePage(m, pg);
								updateButtons(m, pg, skipAmount > 1, fastForward);
							}
							break;
						case CANCEL:
							finalizeEvent(m, success);
							break;
					}

					m = reloadMessage(m);
					modifyButtons(m, Map.of(
							PREVIOUS.name(), b -> p == 0 ? b.asDisabled() : b.asEnabled(),
							SKIP_BACKWARD.name(), b -> p == 0 ? b.asDisabled() : b.asEnabled(),
							GOTO_FIRST.name(), b -> p == 0 ? b.asDisabled() : b.asEnabled(),

							NEXT.name(), b -> p == maxP ? b.asDisabled() : b.asEnabled(),
							SKIP_FORWARD.name(), b -> p == maxP ? b.asDisabled() : b.asEnabled(),
							GOTO_LAST.name(), b -> p == maxP ? b.asDisabled() : b.asEnabled()
					));

					setTimeout(timeout, success, m, time, unit);

					if (wrapper.isFromGuild() && wrapper.getSource() instanceof MessageReactionAddEvent && paginator.isRemoveOnReact()) {
						subGet(((MessageReaction) wrapper.getContent()).removeReaction(u));
					}
				}
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
	 * @param useButtons Whether to use interaction {@link Button} or reactions.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void categorize(@Nonnull Message msg, @Nonnull Map<Emoji, Page> categories, boolean useButtons) throws ErrorResponseException, InsufficientPermissionException {
		categorize(msg, categories, useButtons, 0, null, null);
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
	 * @param categories The categories to be shown. The categories are defined by
	 *                   a {@link Map} containing emoji unicodes or emote ids as keys and
	 *                   {@link Pages} as values.
	 * @param useButtons Whether to use interaction {@link Button} or reactions.
	 * @param time       The time before the listener automatically stop listening
	 *                   for further events (recommended: 60).
	 * @param unit       The time's {@link TimeUnit} (recommended:
	 *                   {@link TimeUnit#SECONDS}).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void categorize(@Nonnull Message msg, @Nonnull Map<Emoji, Page> categories, boolean useButtons, int time, TimeUnit unit) throws ErrorResponseException, InsufficientPermissionException {
		categorize(msg, categories, useButtons, time, unit, null);
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
	 * @param useButtons  Whether to use interaction {@link Button} or reactions.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void categorize(@Nonnull Message msg, @Nonnull Map<Emoji, Page> categories, boolean useButtons, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		categorize(msg, categories, useButtons, 0, null, canInteract);
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
	 * @param useButtons  Whether to use interaction {@link Button} or reactions.
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
	public static void categorize(@Nonnull Message msg, @Nonnull Map<Emoji, Page> categories, boolean useButtons, int time, TimeUnit unit, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		Map<Emoji, Page> cats = Collections.unmodifiableMap(categories);
		clearButtons(msg);
		clearReactions(msg);

		if (useButtons) {
			List<ActionRow> rows = new ArrayList<>();

			List<Component> row = new ArrayList<>();
			for (Emoji k : cats.keySet()) {
				if (row.size() == 5) {
					rows.add(ActionRow.of(row));
					row = new ArrayList<>();
				}

				String id;
				if (k.isCustom()) {
					id = k.getId();
				} else {
					id = k.getName();
				}

				row.add(Button.secondary(id, k));
			}

			Button button = Button.danger(CANCEL.name(), paginator.getEmote(CANCEL));
			if (row.size() == 5) {
				row.set(4, button);
			} else {
				row.add(button);
			}

			rows.add(ActionRow.of(row));
			msg.editMessageComponents(rows).submit();
		} else {
			for (Emoji k : cats.keySet()) {
				msg.addReaction(k.getAsMention().replaceAll("[<>]", "")).submit();
			}

			msg.addReaction(paginator.getStringEmote(CANCEL)).submit();
		}

		handler.addEvent(msg, new ThrowingBiConsumer<>() {
			private Emoji currCat = null;
			private final AtomicReference<ScheduledFuture<?>> timeout = new AtomicReference<>(null);
			private final Consumer<Void> success = s -> {
				if (timeout.get() != null)
					timeout.get().cancel(true);
				handler.removeEvent(msg);
				if (paginator.isDeleteOnCancel()) msg.delete().submit();
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void acceptThrows(@Nonnull User u, @Nonnull PaginationEventWrapper wrapper) {
				Message m = subGet(wrapper.retrieveMessage());

				if (canInteract == null || canInteract.test(u)) {
					if (u.isBot() || m == null || !wrapper.getMessageId().equals(msg.getId()))
						return;

					Emoji emoji = null;
					Emote emt = NONE;
					if (wrapper.getContent() instanceof MessageReaction) {
						MessageReaction.ReactionEmote reaction = ((MessageReaction) wrapper.getContent()).getReactionEmote();
						emoji = toEmoji(reaction);
						emt = toEmote(reaction);
					} else if (wrapper.getContent() instanceof Button) {
						Button btn = (Button) wrapper.getContent();
						emoji = btn.getEmoji();

						if (btn.getId() != null && !btn.getId().contains(".")) {
							emt = Emote.valueOf(btn.getId().replace("*", ""));
						}
					}

					if (emoji == null || emoji.equals(currCat)) return;

					if (emt == CANCEL) {
						finalizeEvent(m, success);
						return;
					}


					setTimeout(timeout, success, m, time, unit);

					Page pg = cats.get(emoji);
					if (pg != null) {
						updatePage(m, pg);
						currCat = emoji;
					}

					modifyButtons(m, Map.of(
							emoji.getId(), Button::asEnabled,
							currCat.getId(), Button::asDisabled
					));

					if (wrapper.isFromGuild() && wrapper.getSource() instanceof MessageReactionAddEvent && paginator.isRemoveOnReact()) {
						subGet(((MessageReaction) wrapper.getContent()).removeReaction(u));
					}
				}
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
	 *                         {@link ThrowingTriConsumer}&lt;{@link Member}, {@link Message}, {@link InteractionHook}&gt;
	 *                         containing desired behavior as value.
	 * @param showCancelButton Should the {@link Emote#CANCEL} button be created automatically?
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void buttonize(@Nonnull Message msg, @Nonnull Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton) throws ErrorResponseException, InsufficientPermissionException {
		buttonize(msg, buttons, useButtons, showCancelButton, 0, null, null, null);
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
	 *                         {@link ThrowingTriConsumer}&lt;{@link Member}, {@link Message}, {@link InteractionHook}&gt;
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
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void buttonize(@Nonnull Message msg, @Nonnull Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton, int time, TimeUnit unit) throws ErrorResponseException, InsufficientPermissionException {
		buttonize(msg, buttons, useButtons, showCancelButton, time, unit, null, null);
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
	 *                         {@link ThrowingTriConsumer}&lt;{@link Member}, {@link Message}, {@link InteractionHook}&gt;
	 *                         containing desired behavior as value.
	 * @param showCancelButton Should the {@link Emote#CANCEL} button be created automatically?
	 * @param canInteract      {@link Predicate} to determine whether the
	 *                         {@link User} that pressed the button can interact
	 *                         with it or not.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void buttonize(@Nonnull Message msg, @Nonnull Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		buttonize(msg, buttons, useButtons, showCancelButton, 0, null, canInteract, null);
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
	 *                         {@link ThrowingTriConsumer}&lt;{@link Member}, {@link Message}, {@link InteractionHook}&gt;
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
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void buttonize(@Nonnull Message msg, @Nonnull Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton, int time, TimeUnit unit, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		buttonize(msg, buttons, useButtons, showCancelButton, time, unit, canInteract, null);
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
	 *                         {@link ThrowingTriConsumer}&lt;{@link Member}, {@link Message}, {@link InteractionHook}&gt;
	 *                         containing desired behavior as value.
	 * @param showCancelButton Should the {@link Emote#CANCEL} button be created automatically?
	 * @param canInteract      {@link Predicate} to determine whether the
	 *                         {@link User} that pressed the button can interact
	 *                         with it or not.
	 * @param onCancel         Action to be run after the listener is removed.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void buttonize(@Nonnull Message msg, @Nonnull Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton, Predicate<User> canInteract, Consumer<Message> onCancel) throws ErrorResponseException, InsufficientPermissionException {
		buttonize(msg, buttons, useButtons, showCancelButton, 0, null, canInteract, onCancel);
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
	 *                         {@link ThrowingTriConsumer}&lt;{@link Member}, {@link Message}, {@link InteractionHook}&gt;
	 *                         containing desired behavior as value.
	 * @param showCancelButton Should the {@link Emote#CANCEL} button be created automatically?
	 * @param time             The time before the listener automatically stop
	 *                         listening for further events (recommended: 60).
	 * @param unit             The time's {@link TimeUnit} (recommended:
	 *                         {@link TimeUnit#SECONDS}).
	 * @param canInteract      {@link Predicate} to determine whether the
	 *                         {@link User} that pressed the button can interact
	 *                         with it or not.
	 * @param onCancel         Action to be run after the listener is removed.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static void buttonize(@Nonnull Message msg, @Nonnull Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton, int time, TimeUnit unit, Predicate<User> canInteract, Consumer<Message> onCancel) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		Map<Emoji, ThrowingConsumer<ButtonWrapper>> btns = Collections.unmodifiableMap(buttons);
		clearButtons(msg);
		clearReactions(msg);

		if (useButtons) {
			List<ActionRow> rows = new ArrayList<>();

			List<Component> row = new ArrayList<>();
			for (Emoji k : btns.keySet()) {
				if (row.size() == 5) {
					rows.add(ActionRow.of(row));
					row = new ArrayList<>();
				}

				String id;
				if (k.isCustom()) {
					id = k.getId();
				} else {
					id = k.getName();
				}

				Button button = Button.secondary(id, k);
				row.add(button);
			}

			if (!btns.containsKey(paginator.getEmote(CANCEL)) && showCancelButton) {
				Button button = Button.danger(CANCEL.name(), paginator.getEmote(CANCEL));

				if (row.size() == 5) {
					row.set(4, button);
				} else {
					row.add(button);
				}
			}

			rows.add(ActionRow.of(row));
			msg.editMessageComponents(rows).submit();
		} else {
			for (Emoji k : btns.keySet()) {
				msg.addReaction(k.getAsMention().replaceAll("[<>]", "")).submit();
			}

			if (!btns.containsKey(paginator.getEmote(CANCEL)) && showCancelButton)
				msg.addReaction(paginator.getStringEmote(CANCEL)).submit();
		}

		handler.addEvent(msg, new ThrowingBiConsumer<>() {
			private final AtomicReference<ScheduledFuture<?>> timeout = new AtomicReference<>(null);
			private final Consumer<Void> success = s -> {
				if (timeout.get() != null)
					timeout.get().cancel(true);
				handler.removeEvent(msg);
				if (onCancel != null) onCancel.accept(msg);
				if (paginator.isDeleteOnCancel()) msg.delete().submit();
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void acceptThrows(@Nonnull User u, @Nonnull PaginationEventWrapper wrapper) {
				Message m = subGet(wrapper.retrieveMessage());

				if (canInteract == null || canInteract.test(u)) {
					if (u.isBot() || m == null || !wrapper.getMessageId().equals(msg.getId()))
						return;

					Emoji emoji = null;
					Emote emt = NONE;
					if (wrapper.getContent() instanceof MessageReaction) {
						MessageReaction.ReactionEmote reaction = ((MessageReaction) wrapper.getContent()).getReactionEmote();
						emoji = toEmoji(reaction);
						emt = toEmote(reaction);
					} else if (wrapper.getContent() instanceof Button) {
						Button btn = (Button) wrapper.getContent();
						emoji = btn.getEmoji();

						if (btn.getId() != null && !Emote.isCustom(btn) && !btn.getId().contains(".")) {
							emt = Emote.valueOf(btn.getId().replace("*", ""));
						}
					}

					if ((!btns.containsKey(paginator.getEmote(CANCEL)) && showCancelButton) && emt == CANCEL) {
						finalizeEvent(m, success);
						return;
					}

					InteractionHook hook;
					if (wrapper.getSource() instanceof ButtonClickEvent) {
						hook = ((ButtonClickEvent) wrapper.getSource()).getHook();
					} else {
						hook = null;
					}

					btns.get(emoji).accept(new ButtonWrapper(wrapper.getMember(), hook, m));

					setTimeout(timeout, success, m, time, unit);

					if (wrapper.isFromGuild() && wrapper.getSource() instanceof MessageReactionAddEvent && paginator.isRemoveOnReact()) {
						subGet(((MessageReaction) wrapper.getContent()).removeReaction(u));
					}
				}
			}
		});
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pageLoader {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                   returns null the method will treat it as last page, preventing unnecessary updates.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static void lazyPaginate(@Nonnull Message msg, @Nonnull ThrowingFunction<Integer, Page> pageLoader) throws ErrorResponseException, InsufficientPermissionException {
		lazyPaginate(msg, pageLoader, false, 0, null, null);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit
	 * You can specify how long the listener will stay active before shutting down itself after a
	 * no-activity interval.
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pageLoader {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                   returns null the method will treat it as last page, preventing unnecessary updates.
	 * @param time       The time before the listener automatically stop listening
	 *                   for further events (recommended: 60).
	 * @param unit       The time's {@link TimeUnit} (recommended:
	 *                   {@link TimeUnit#SECONDS}).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static void lazyPaginate(@Nonnull Message msg, @Nonnull ThrowingFunction<Integer, Page> pageLoader, int time, TimeUnit unit) throws ErrorResponseException, InsufficientPermissionException {
		lazyPaginate(msg, pageLoader, false, time, unit, null);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pageLoader  {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                    returns null the method will treat it as last page, preventing unnecessary updates.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static void lazyPaginate(@Nonnull Message msg, @Nonnull ThrowingFunction<Integer, Page> pageLoader, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		lazyPaginate(msg, pageLoader, false, 0, null, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit
	 * You can specify how long the listener will stay active before shutting down itself after a
	 * no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pageLoader  {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                    returns null the method will treat it as last page, preventing unnecessary updates.
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
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static void lazyPaginate(@Nonnull Message msg, @Nonnull ThrowingFunction<Integer, Page> pageLoader, int time, TimeUnit unit, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		lazyPaginate(msg, pageLoader, false, time, unit, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pageLoader {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                   returns null the method will treat it as last page, preventing unnecessary updates.
	 * @param cache      Enables {@link Page} caching, saving previously visited pages.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static void lazyPaginate(@Nonnull Message msg, @Nonnull ThrowingFunction<Integer, Page> pageLoader, boolean cache) throws ErrorResponseException, InsufficientPermissionException {
		lazyPaginate(msg, pageLoader, cache, 0, null, null);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit
	 * You can specify how long the listener will stay active before shutting down itself after a
	 * no-activity interval.
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pageLoader {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                   returns null the method will treat it as last page, preventing unnecessary updates.
	 * @param cache      Enables {@link Page} caching, saving previously visited pages.
	 * @param time       The time before the listener automatically stop listening
	 *                   for further events (recommended: 60).
	 * @param unit       The time's {@link TimeUnit} (recommended:
	 *                   {@link TimeUnit#SECONDS}).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static void lazyPaginate(@Nonnull Message msg, @Nonnull ThrowingFunction<Integer, Page> pageLoader, boolean cache, int time, TimeUnit unit) throws ErrorResponseException, InsufficientPermissionException {
		lazyPaginate(msg, pageLoader, cache, time, unit, null);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pageLoader  {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                    returns null the method will treat it as last page, preventing unnecessary updates.
	 * @param cache       Enables {@link Page} caching, saving previously visited pages.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static void lazyPaginate(@Nonnull Message msg, @Nonnull ThrowingFunction<Integer, Page> pageLoader, boolean cache, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		lazyPaginate(msg, pageLoader, cache, 0, null, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit
	 * You can specify how long the listener will stay active before shutting down itself after a
	 * no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pageLoader  {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                    returns null the method will treat it as last page, preventing unnecessary updates.
	 * @param cache       Enables {@link Page} caching, saving previously visited pages.
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
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static void lazyPaginate(@Nonnull Message msg, @Nonnull ThrowingFunction<Integer, Page> pageLoader, boolean cache, int time, TimeUnit unit, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		clearButtons(msg);
		clearReactions(msg);

		List<Page> pageCache = cache ? new ArrayList<>() : null;

		Page pg = pageLoader.apply(0);
		if (pg == null) {
			throw new InvalidStateException();
		}

		if (cache) pageCache.add(pg);

		if (pg instanceof InteractPage) addButtons((InteractPage) pg, msg, false, false);
		else addReactions(msg, false, false);

		handler.addEvent(msg, new ThrowingBiConsumer<>() {
			private int p = 0;
			private final AtomicReference<ScheduledFuture<?>> timeout = new AtomicReference<>(null);
			private final Consumer<Void> success = s -> {
				if (timeout.get() != null)
					timeout.get().cancel(true);
				handler.removeEvent(msg);
				if (paginator.isDeleteOnCancel()) msg.delete().submit();
			};

			{
				setTimeout(timeout, success, msg, time, unit);
			}

			@Override
			public void acceptThrows(@Nonnull User u, @Nonnull PaginationEventWrapper wrapper) {
				Message m = subGet(wrapper.retrieveMessage());

				if (canInteract == null || canInteract.test(u)) {
					if (u.isBot() || m == null || !wrapper.getMessageId().equals(msg.getId()))
						return;

					Emote emt = NONE;
					if (wrapper.getContent() instanceof MessageReaction) {
						MessageReaction.ReactionEmote reaction = ((MessageReaction) wrapper.getContent()).getReactionEmote();
						emt = toEmote(reaction);
					} else if (wrapper.getContent() instanceof Button) {
						String id = ((Button) wrapper.getContent()).getId();
						if (id != null && !id.contains(".")) {
							emt = Emote.valueOf(id.replace("*", ""));
						}
					}

					switch (emt) {
						case PREVIOUS:
							if (p > 0) {
								p--;
								Page pg = cache ? pageCache.get(p) : pageLoader.apply(p);

								updatePage(m, pg);
								updateButtons(m, pg, false, false);
							}
							break;
						case NEXT:
							p++;
							Page pg;
							if (cache && pageCache.size() > p) {
								pg = pageCache.get(p);
							} else {
								pg = pageLoader.apply(p);
								if (pg == null) {
									p--;
									return;
								}
							}

							if (cache) pageCache.add(pg);

							updatePage(m, pg);
							updateButtons(m, pg, false, false);
							break;
						case CANCEL:
							finalizeEvent(m, success);
							break;
					}

					setTimeout(timeout, success, m, time, unit);

					if (wrapper.isFromGuild() && wrapper.getSource() instanceof MessageReactionAddEvent && paginator.isRemoveOnReact()) {
						subGet(((MessageReaction) wrapper.getContent()).removeReaction(u));
					}
				}
			}
		});
	}

	/**
	 * Method used to update the current page.
	 * <strong>Must not be called outside of {@link Pages}</strong>.
	 *
	 * @param msg The current {@link Message} object.
	 * @param p   The current {@link Page}.
	 */
	private static void updatePage(@Nonnull Message msg, Page p) {
		if (p == null) throw new NullPageException();

		if (p.getContent() instanceof Message) {
			msg.editMessage((Message) p.getContent()).submit();
		} else if (p.getContent() instanceof MessageEmbed) {
			msg.editMessageEmbeds((MessageEmbed) p.getContent()).submit();
		}
	}

	private static void updateButtons(Message msg, Page pg, boolean withSkip, boolean withGoto) {
		if (pg instanceof InteractPage) {
			addButtons((InteractPage) pg, msg, withSkip, withGoto);
		} else {
			addReactions(msg, withSkip, withGoto);
		}
	}

	/**
	 * Method used to set expiration of the events.
	 * <strong>Must not be called outside of {@link Pages}</strong>.
	 *
	 * @param timeout The {@link CompletableFuture} reference which will contain expiration action.
	 * @param success The {@link Consumer} to be called after expiration.
	 * @param msg     The {@link Message} related to this event.
	 * @param time    How much time before expiration.
	 * @param unit    The {@link TimeUnit} for the expiration time.
	 */
	private static void setTimeout(AtomicReference<ScheduledFuture<?>> timeout, Consumer<Void> success, Message msg, int time, TimeUnit unit) {
		if (timeout.get() != null)
			timeout.get().cancel(true);

		if (time <= 0 || unit == null) return;
		try {
			timeout.set(
					executor.schedule(() -> {
						msg.clearReactions().submit().thenAccept(success);
					}, time, unit)
			);
		} catch (InsufficientPermissionException | IllegalStateException e) {
			timeout.set(
					executor.schedule(() -> {
						msg.getChannel()
								.retrieveMessageById(msg.getId())
								.submit()
								.thenCompose(m -> {
									CompletableFuture<?>[] removeReaction = new CompletableFuture[m.getReactions().size()];

									for (int i = 0; i < m.getReactions().size(); i++) {
										MessageReaction r = m.getReactions().get(i);

										if (!r.isSelf()) continue;

										removeReaction[i] = r.removeReaction().submit();
									}

									return CompletableFuture.allOf(removeReaction).thenAccept(success);
								});
					}, time, unit)
			);
		}
	}

	public static Message reloadMessage(Message msg) {
		return subGet(msg.getChannel().retrieveMessageById(msg.getId()), msg);
	}

	public static <T> T subGet(RestAction<T> future) {
		try {
			return future.submit().get();
		} catch (InterruptedException | ExecutionException e) {
			Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Exception during future execution:", e);
			return null;
		}
	}

	public static <T> T subGet(RestAction<T> future, T or) {
		try {
			return future.submit().get();
		} catch (InterruptedException | ExecutionException e) {
			Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_4, "Exception during future execution:", e);
			return or;
		}
	}

	private static Emote toEmote(MessageReaction.ReactionEmote reaction) {
		return Emote.getByEmoji(toEmoji(reaction));
	}

	private static Emoji toEmoji(MessageReaction.ReactionEmote reaction) {
		return reaction.isEmoji() ? Emoji.fromUnicode(reaction.getEmoji()) : Emoji.fromEmote(reaction.getEmote());
	}

	/**
	 * Utility method to clear all reactions of a message.
	 *
	 * @param msg The {@link Message} to have reactions/buttons removed from.
	 */
	public static void clearReactions(Message msg) {
		try {
			if (msg.getChannel().getType().isGuild())
				msg.clearReactions().submit();
			else for (MessageReaction r : msg.getReactions()) {
				r.removeReaction().submit();
			}
		} catch (InsufficientPermissionException | IllegalStateException e) {
			for (MessageReaction r : msg.getReactions()) {
				r.removeReaction().submit();
			}
		}
	}

	/**
	 * Utility method to clear all buttons of a message.
	 *
	 * @param msg The {@link Message} to have reactions/buttons removed from.
	 */
	public static void clearButtons(Message msg) {
		try {
			subGet(msg.editMessageComponents());
		} catch (InsufficientPermissionException | IllegalStateException e) {
			Pages.getPaginator().log(PUtilsConfig.LogLevel.LEVEL_3, "Not enough permissions to clear message reactions:", e);
		}
	}

	/**
	 * Utility method to clear all reactions of a message.
	 *
	 * @param msg      The {@link Message} to have reactions removed from.
	 * @param callback Action to be executed after removing reactions.
	 */
	public static void finalizeEvent(Message msg, Consumer<Void> callback) {
		clearButtons(msg);
		clearReactions(msg);

		callback.accept(null);
	}

	public static void modifyButtons(Message msg, Map<String, Function<Button, Button>> changes) {
		List<ActionRow> rows = new ArrayList<>(msg.getActionRows());

		for (ActionRow ar : rows) {
			List<Component> row = ar.getComponents();
			for (int i = 0; i < row.size(); i++) {
				Component c = row.get(i);
				if (c instanceof Button && changes.containsKey(c.getId())) {
					row.set(i, changes.get(c.getId()).apply((Button) c));
				}
			}
		}

		subGet(msg.editMessageComponents(rows));
	}

	/**
	 * Utility method to add navigation buttons.
	 *
	 * @param msg The {@link Message} to have reactions removed from.
	 */
	public static void addReactions(Message msg, boolean withSkip, boolean withGoto) {
		clearButtons(msg);

		if (withGoto) msg.addReaction(paginator.getStringEmote(GOTO_FIRST)).submit();
		if (withSkip) msg.addReaction(paginator.getStringEmote(SKIP_BACKWARD)).submit();

		msg.addReaction(paginator.getStringEmote(PREVIOUS)).submit();
		msg.addReaction(paginator.getStringEmote(CANCEL)).submit();
		msg.addReaction(paginator.getStringEmote(NEXT)).submit();

		if (withSkip) msg.addReaction(paginator.getStringEmote(SKIP_FORWARD)).submit();
		if (withGoto) msg.addReaction(paginator.getStringEmote(GOTO_LAST)).submit();
	}

	/**
	 * Utility method to add navigation buttons.
	 *
	 * @param msg The {@link Message} to have reactions removed from.
	 */
	public static void addButtons(InteractPage page, Message msg, boolean withSkip, boolean withGoto) {
		clearReactions(msg);

		List<ActionRow> rows = new ArrayList<>();

		List<Component> row = List.of(
				page.makeButton(paginator, NONE),
				page.makeButton(paginator, PREVIOUS),
				page.makeButton(paginator, CANCEL),
				page.makeButton(paginator, NEXT),
				page.makeButton(paginator, NONE)
		);
		rows.add(ActionRow.of(row));

		if (withSkip || withGoto) {
			row = List.of(
					page.makeButton(paginator, withGoto ? GOTO_FIRST : NONE),
					page.makeButton(paginator, withSkip ? SKIP_BACKWARD : NONE),
					page.makeButton(paginator, NONE),
					page.makeButton(paginator, withSkip ? SKIP_FORWARD : NONE),
					page.makeButton(paginator, withGoto ? GOTO_LAST : NONE)
			);
			rows.add(ActionRow.of(row));
		}

		subGet(msg.editMessageComponents(rows));
	}
}
