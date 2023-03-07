package com.github.ygimenez.method;

import com.github.ygimenez.exception.AlreadyActivatedException;
import com.github.ygimenez.exception.InvalidHandlerException;
import com.github.ygimenez.exception.InvalidStateException;
import com.github.ygimenez.listener.MessageHandler;
import com.github.ygimenez.model.*;
import com.github.ygimenez.model.helper.*;
import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.ygimenez.type.Emote.*;

/**
 * The main class containing all pagination-related methods, including but not limited
 * to {@link #paginate}, {@link #categorize}, {@link #buttonize} and {@link #lazyPaginate}.
 */
public abstract class Pages {
	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private static final MessageHandler handler = new MessageHandler();
	private static Paginator paginator;

	private Pages() {
	}

	/**
	 * Set a {@link Paginator} object to handle incoming events. This is
	 * required only once unless you want to change which client is handling events. <br>
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
	public static void activate(@NotNull Paginator paginator) throws InvalidHandlerException {
		if (isActivated()) throw new AlreadyActivatedException();

		Object hand = paginator.getHandler();
		if (hand instanceof JDA) {
			((JDA) hand).addEventListener(handler);
		} else if (hand instanceof ShardManager) {
			((ShardManager) hand).addEventListener(handler);
		} else {
			throw new InvalidHandlerException();
		}

		Pages.paginator = paginator;
		paginator.log(PUtilsConfig.LogLevel.LEVEL_2, "Pagination Utils activated successfully");
	}

	/**
	 * Removes current button handler, allowing another {@link #activate(Paginator)} call.<br>
	 * <br>
	 * Using this method without activating beforehand will do nothing.
	 */
	public static void deactivate() {
		if (!isActivated()) return;

		Object hand = paginator.getHandler();
		if (hand instanceof JDA) {
			((JDA) hand).removeEventListener(handler);
		} else if (hand instanceof ShardManager) {
			((ShardManager) hand).removeEventListener(handler);
		}

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
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pages      The pages to be shown. The order of the {@link List} will
	 *                   define the order of the pages.
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, 0, null, 0, false, null);
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
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @param time       The time before the listener automatically stop listening
	 *                   for further events (recommended: 60).
	 * @param unit       The time's {@link TimeUnit} (recommended:
	 *                   {@link TimeUnit#SECONDS}).
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int time, TimeUnit unit) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, time, unit, 0, false, null);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, 0, null, 0, false, canInteract);
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
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int time, TimeUnit unit, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, time, unit, 0, false, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param fastForward Whether the {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST} buttons should be shown.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, boolean fastForward) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, 0, null, 0, fastForward, null);
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
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param fastForward Whether the {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST} buttons should be shown.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int time, TimeUnit unit, boolean fastForward) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, time, unit, 0, fastForward, null);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param fastForward Whether the {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST} buttons should be shown.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, boolean fastForward, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, 0, null, 0, fastForward, canInteract);
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
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param fastForward Whether the {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST} buttons should be shown.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int time, TimeUnit unit, boolean fastForward, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, time, unit, 0, fastForward, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pages      The pages to be shown. The order of the {@link List} will
	 *                   define the order of the pages.
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @param skipAmount The amount of pages to be skipped when clicking {@link Emote#SKIP_BACKWARD}
	 *                   and {@link Emote#SKIP_FORWARD} buttons.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int skipAmount) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, 0, null, skipAmount, false, null);
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
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @param time       The time before the listener automatically stop listening
	 *                   for further events (recommended: 60).
	 * @param unit       The time's {@link TimeUnit} (recommended:
	 *                   {@link TimeUnit#SECONDS}).
	 * @param skipAmount The amount of pages to be skipped when clicking {@link Emote#SKIP_BACKWARD}
	 *                   and {@link Emote#SKIP_FORWARD} buttons.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int time, TimeUnit unit, int skipAmount) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, time, unit, skipAmount, false, null);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param skipAmount  The amount of pages to be skipped when clicking {@link Emote#SKIP_BACKWARD}
	 *                    and {@link Emote#SKIP_FORWARD} buttons.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int skipAmount, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, 0, null, skipAmount, false, canInteract);
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
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param skipAmount  The amount of pages to be skipped when clicking {@link Emote#SKIP_BACKWARD}
	 *                    and {@link Emote#SKIP_FORWARD} buttons.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int time, TimeUnit unit, int skipAmount, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, time, unit, skipAmount, false, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param skipAmount  The amount of pages to be skipped when clicking {@link Emote#SKIP_BACKWARD}
	 *                    and {@link Emote#SKIP_FORWARD} buttons.
	 * @param fastForward Whether the {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST} buttons should be shown.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int skipAmount, boolean fastForward, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, 0, null, skipAmount, fastForward, canInteract);
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
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param skipAmount  The amount of pages to be skipped when clicking {@link Emote#SKIP_BACKWARD}
	 *                    and {@link Emote#SKIP_FORWARD} buttons.
	 * @param fastForward Whether the {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST} buttons should be shown.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int time, TimeUnit unit, int skipAmount, boolean fastForward) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, time, unit, skipAmount, fastForward, null);
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
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param skipAmount  The amount of pages to be skipped when clicking {@link Emote#SKIP_BACKWARD}
	 *                    and {@link Emote#SKIP_FORWARD} buttons.
	 * @param fastForward Whether the {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST} buttons should be shown.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int time, TimeUnit unit, int skipAmount, boolean fastForward, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, new PaginateHelper(pages, useButtons)
				.setTimeout(time, unit)
				.setSkipAmount(skipAmount)
				.setFastForward(fastForward)
				.setCanInteract(canInteract)
		);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. This versions uses a helper class
	 * to aid customization and allow reusage of configurations.
	 *
	 * @param msg    The {@link Message} sent which will be paginated.
	 * @param helper A {@link PaginateHelper} holding desired pagination settings.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (ie. garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull PaginateHelper helper) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated() || helper.getContent().isEmpty()) throw new InvalidStateException();
		boolean useBtns = helper.isUsingButtons() && msg.getAuthor().getId().equals(msg.getJDA().getSelfUser().getId());
		List<Page> pgs = Collections.unmodifiableList(helper.getContent());

		if (useBtns && helper.shouldUpdate(msg)) {
			helper.apply(msg.editMessageComponents()).submit();
		} else if (!useBtns) {
			clearButtons(msg);
			clearReactions(msg);
			addReactions(msg, helper.getSkipAmount() > 1, helper.isFastForward());
		}

		return handler.addEvent(msg, new ThrowingBiConsumer<>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private ScheduledFuture<?> timeout;
			private final Consumer<Void> success = s -> {
				if (timeout != null) {
					timeout.cancel(true);
				}

				handler.removeEvent(msg);
				if (paginator.isDeleteOnCancel()) msg.delete().submit();
			};

			private final Function<Button, Button> LOWER_BOUNDARY_CHECK = b -> b.withDisabled(p == 0);
			private final Function<Button, Button> UPPER_BOUNDARY_CHECK = b -> b.withDisabled(p == maxP);

			{
				if (helper.getTimeout() > 0) {
					timeout = executor.schedule(() -> finalizeEvent(msg, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
				}
			}

			@Override
			public void acceptThrows(@NotNull User u, @NotNull PaginationEventWrapper wrapper) {
				Message m = subGet(wrapper.retrieveMessage());

				if (helper.canInteract(u)) {
					if (u.isBot() || m == null || !wrapper.getMessageId().equals(msg.getId())) return;

					Emote emt = NONE;
					if (wrapper.getContent() instanceof MessageReaction) {
						EmojiUnion reaction = ((MessageReaction) wrapper.getContent()).getEmoji();
						emt = toEmote(reaction);
					} else if (wrapper.getContent() instanceof Button) {
						Button btn = (Button) wrapper.getContent();

						if (btn.getId() != null && Emote.isNative(btn) && !btn.getId().contains(".")) {
							emt = Emote.valueOf(btn.getId().replace("*", ""));
						}
					}

					Page pg = null;
					boolean update = false;
					switch (emt) {
						case PREVIOUS:
							if (p > 0) {
								p--;
								update = true;
							}
							break;
						case NEXT:
							if (p < maxP) {
								p++;
								update = true;
							}
							break;
						case SKIP_BACKWARD:
							if (p > 0) {
								p -= (p - helper.getSkipAmount() < 0 ? p : helper.getSkipAmount());
								update = true;
							}
							break;
						case SKIP_FORWARD:
							if (p < maxP) {
								p += (p + helper.getSkipAmount() > maxP ? maxP - p : helper.getSkipAmount());
								update = true;
							}
							break;
						case GOTO_FIRST:
							if (p > 0) {
								p = 0;
								update = true;
							}
							break;
						case GOTO_LAST:
							if (p < maxP) {
								p = maxP;
								update = true;
							}
							break;
						case CANCEL:
							finalizeEvent(m, success);
							return;
					}

					if (update) {
						pg = pgs.get(p);
						modifyButtons(m, pg, helper, Map.of(
								PREVIOUS.name(), LOWER_BOUNDARY_CHECK,
								SKIP_BACKWARD.name(), LOWER_BOUNDARY_CHECK,
								GOTO_FIRST.name(), LOWER_BOUNDARY_CHECK,

								NEXT.name(), UPPER_BOUNDARY_CHECK,
								SKIP_FORWARD.name(), UPPER_BOUNDARY_CHECK,
								GOTO_LAST.name(), UPPER_BOUNDARY_CHECK
						));
					}

					if (timeout != null) {
						timeout.cancel(true);
					}
					if (helper.getTimeout() > 0) {
						timeout = executor.schedule(() -> finalizeEvent(m, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
					}

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
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static ActionReference categorize(@NotNull Message msg, @NotNull Map<Emoji, Page> categories, boolean useButtons) throws ErrorResponseException, InsufficientPermissionException {
		return categorize(msg, categories, useButtons, 0, null, null);
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
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @param time       The time before the listener automatically stop listening
	 *                   for further events (recommended: 60).
	 * @param unit       The time's {@link TimeUnit} (recommended:
	 *                   {@link TimeUnit#SECONDS}).
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static ActionReference categorize(@NotNull Message msg, @NotNull Map<Emoji, Page> categories, boolean useButtons, int time, TimeUnit unit) throws ErrorResponseException, InsufficientPermissionException {
		return categorize(msg, categories, useButtons, time, unit, null);
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
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static ActionReference categorize(@NotNull Message msg, @NotNull Map<Emoji, Page> categories, boolean useButtons, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return categorize(msg, categories, useButtons, 0, null, canInteract);
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
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static ActionReference categorize(@NotNull Message msg, @NotNull Map<Emoji, Page> categories, boolean useButtons, int time, TimeUnit unit, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return categorize(msg, new CategorizeHelper(categories, useButtons)
				.setTimeout(time, unit)
				.setCanInteract(canInteract)
		);
	}

	/**
	 * Adds menu-like buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will browse through a given {@link Map} of pages. You may only specify
	 * one {@link Page} per button, adding another button with an existing unicode
	 * will overwrite the current button's {@link Page}. This versions uses a helper class
	 * to aid customization and allow reusage of configurations.
	 *
	 * @param msg    The {@link Message} sent which will be categorized.
	 * @param helper A {@link CategorizeHelper} holding desired categorization settings.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static ActionReference categorize(@NotNull Message msg, @NotNull CategorizeHelper helper) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		boolean useBtns = helper.isUsingButtons() && msg.getAuthor().getId().equals(msg.getJDA().getSelfUser().getId());

		Map<Emoji, Page> cats = Collections.unmodifiableMap(helper.getContent());

		if (useBtns && helper.shouldUpdate(msg)) {
			helper.apply(msg.editMessageComponents()).submit();
		} else if (!useBtns) {
			clearButtons(msg);
			clearReactions(msg);

			for (Emoji k : cats.keySet()) {
				msg.addReaction(k).submit();
			}

			msg.addReaction(paginator.getEmoji(CANCEL)).submit();
		}

		return handler.addEvent(msg, new ThrowingBiConsumer<>() {
			private Emoji currCat = null;
			private ScheduledFuture<?> timeout;
			private final Consumer<Void> success = s -> {
				if (timeout != null) {
					timeout.cancel(true);
				}

				handler.removeEvent(msg);
				if (paginator.isDeleteOnCancel()) msg.delete().submit();
			};

			{
				if (helper.getTimeout() > 0) {
					timeout = executor.schedule(() -> finalizeEvent(msg, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
				}
			}

			@Override
			public void acceptThrows(@NotNull User u, @NotNull PaginationEventWrapper wrapper) {
				Message m = subGet(wrapper.retrieveMessage());

				if (helper.canInteract(u)) {
					if (u.isBot() || m == null || !wrapper.getMessageId().equals(msg.getId())) return;

					Emoji emoji = null;
					Emote emt = NONE;
					if (wrapper.getContent() instanceof MessageReaction) {
						EmojiUnion reaction = ((MessageReaction) wrapper.getContent()).getEmoji();
						emoji = toEmoji(reaction);
						emt = toEmote(reaction);
					} else if (wrapper.getContent() instanceof Button) {
						Button btn = (Button) wrapper.getContent();
						emoji = btn.getEmoji();

						if (btn.getId() != null && Emote.isNative(btn) && !btn.getId().contains(".")) {
							emt = Emote.valueOf(btn.getId().replace("*", ""));
						}
					}

					if (emt == CANCEL) {
						finalizeEvent(m, success);
						return;
					} else if (emoji != null && !Objects.equals(emoji, currCat)) {
						Page pg = lookupValue(cats, emoji);
						if (pg != null) {
							currCat = emoji;
							modifyButtons(m, pg, helper, Map.of(Emote.getId(currCat), Button::asDisabled));
						}
					}

					if (timeout != null) {
						timeout.cancel(true);
					}
					if (helper.getTimeout() > 0) {
						timeout = executor.schedule(() -> finalizeEvent(msg, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
					}

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
	 * @param useButtons       Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                         {@link Message} was not sent by the bot.
	 * @param showCancelButton Should the {@link Emote#CANCEL} button be created automatically?
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static ActionReference buttonize(@NotNull Message msg, @NotNull Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton) throws ErrorResponseException, InsufficientPermissionException {
		return buttonize(msg, buttons, useButtons, showCancelButton, 0, null, null, null);
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
	 * @param useButtons       Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                         {@link Message} was not sent by the bot.
	 * @param showCancelButton Should the {@link Emote#CANCEL} button be created automatically?
	 * @param time             The time before the listener automatically stop
	 *                         listening for further events (recommended: 60).
	 * @param unit             The time's {@link TimeUnit} (recommended:
	 *                         {@link TimeUnit#SECONDS}).
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static ActionReference buttonize(@NotNull Message msg, @NotNull Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton, int time, TimeUnit unit) throws ErrorResponseException, InsufficientPermissionException {
		return buttonize(msg, buttons, useButtons, showCancelButton, time, unit, null, null);
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
	 * @param useButtons       Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                         {@link Message} was not sent by the bot.
	 * @param showCancelButton Should the {@link Emote#CANCEL} button be created automatically?
	 * @param canInteract      {@link Predicate} to determine whether the
	 *                         {@link User} that pressed the button can interact
	 *                         with it or not.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static ActionReference buttonize(@NotNull Message msg, @NotNull Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return buttonize(msg, buttons, useButtons, showCancelButton, 0, null, canInteract, null);
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
	 * @param useButtons       Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                         {@link Message} was not sent by the bot.
	 * @param showCancelButton Should the {@link Emote#CANCEL} button be created automatically?
	 * @param time             The time before the listener automatically stop
	 *                         listening for further events (recommended: 60).
	 * @param unit             The time's {@link TimeUnit} (recommended:
	 *                         {@link TimeUnit#SECONDS}).
	 * @param canInteract      {@link Predicate} to determine whether the
	 *                         {@link User} that pressed the button can interact
	 *                         with it or not.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static ActionReference buttonize(@NotNull Message msg, @NotNull Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton, int time, TimeUnit unit, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return buttonize(msg, buttons, useButtons, showCancelButton, time, unit, canInteract, null);
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
	 * @param useButtons       Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                         {@link Message} was not sent by the bot.
	 * @param showCancelButton Should the {@link Emote#CANCEL} button be created automatically?
	 * @param canInteract      {@link Predicate} to determine whether the
	 *                         {@link User} that pressed the button can interact
	 *                         with it or not.
	 * @param onCancel         Action to be run after the listener is removed.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static ActionReference buttonize(@NotNull Message msg, @NotNull Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton, Predicate<User> canInteract, Consumer<Message> onCancel) throws ErrorResponseException, InsufficientPermissionException {
		return buttonize(msg, buttons, useButtons, showCancelButton, 0, null, canInteract, onCancel);
	}

	/**
	 * Adds buttons to the specified {@link Message}/{@link MessageEmbed}, with each
	 * executing a specific task on click. You may only specify one {@link Runnable}
	 * per button, adding another button with an existing unicode will overwrite the
	 * current button's {@link Runnable}. You can specify the time in which the
	 * listener will automatically stop itself after a no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be buttoned.
	 * @param buttons     The buttons to be shown. The buttons are defined by a
	 *                    Map containing emoji unicodes or emote ids as keys and
	 *                    {@link ThrowingTriConsumer}&lt;{@link Member}, {@link Message}, {@link InteractionHook}&gt;
	 *                    containing desired behavior as value.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param cancellable Should the {@link Emote#CANCEL} button be created automatically?
	 * @param time        The time before the listener automatically stop
	 *                    listening for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param canInteract {@link Predicate} to determine whether the
	 *                    {@link User} that pressed the button can interact
	 *                    with it or not.
	 * @param onCancel    Action to be run after the listener is removed.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static ActionReference buttonize(@NotNull Message msg, @NotNull Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean cancellable, int time, TimeUnit unit, Predicate<User> canInteract, Consumer<Message> onCancel) throws ErrorResponseException, InsufficientPermissionException {
		return buttonize(msg, new ButtonizeHelper(buttons, useButtons)
				.setCancellable(cancellable)
				.setTimeout(time, unit)
				.setCanInteract(canInteract)
				.setOnFinalization(onCancel)
		);
	}

	/**
	 * Adds buttons to the specified {@link Message}/{@link MessageEmbed}, with each
	 * executing a specific task on click. You may only specify one {@link Runnable}
	 * per button, adding another button with an existing unicode will overwrite the
	 * current button's {@link Runnable}. This versions uses a helper class
	 * to aid customization and allow reusage of configurations.
	 *
	 * @param msg    The {@link Message} sent which will be buttoned.
	 * @param helper A {@link ButtonizeHelper} holding desired buttonization settings.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static ActionReference buttonize(@NotNull Message msg, @NotNull ButtonizeHelper helper) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		boolean useBtns = helper.isUsingButtons() && msg.getAuthor().getId().equals(msg.getJDA().getSelfUser().getId());

		Map<Emoji, ThrowingConsumer<ButtonWrapper>> btns = Collections.unmodifiableMap(helper.getContent());

		if (useBtns && helper.shouldUpdate(msg)) {
			helper.apply(msg.editMessageComponents()).submit();
		} else if (!useBtns) {
			clearButtons(msg);
			clearReactions(msg);

			for (Emoji k : btns.keySet()) {
				msg.addReaction(k).submit();
			}

			if (!btns.containsKey(paginator.getEmoji(CANCEL)) && helper.isCancellable()) {
				msg.addReaction(paginator.getEmoji(CANCEL)).submit();
			}
		}

		return handler.addEvent(msg, new ThrowingBiConsumer<>() {
			private ScheduledFuture<?> timeout;
			private final Consumer<Void> success = s -> {
				if (timeout != null) {
					timeout.cancel(true);
				}

				handler.removeEvent(msg);
				if (helper.getOnFinalization() != null) helper.getOnFinalization().accept(msg);
				if (paginator.isDeleteOnCancel()) msg.delete().submit();
			};

			{
				if (helper.getTimeout() > 0) {
					timeout = executor.schedule(() -> finalizeEvent(msg, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
				}
			}

			@Override
			public void acceptThrows(@NotNull User u, @NotNull PaginationEventWrapper wrapper) {
				Message m = subGet(wrapper.retrieveMessage());

				if (helper.canInteract(u)) {
					if (u.isBot() || m == null || !wrapper.getMessageId().equals(msg.getId())) return;

					Emoji emoji = null;
					Emote emt = NONE;
					if (wrapper.getContent() instanceof MessageReaction) {
						EmojiUnion reaction = ((MessageReaction) wrapper.getContent()).getEmoji();
						emoji = toEmoji(reaction);
						emt = toEmote(reaction);
					} else if (wrapper.getContent() instanceof Button) {
						Button btn = (Button) wrapper.getContent();
						emoji = btn.getEmoji();

						if (btn.getId() != null && Emote.isNative(btn) && !btn.getId().contains(".")) {
							emt = Emote.valueOf(btn.getId().replace("*", ""));
						}
					}

					if ((!btns.containsKey(paginator.getEmoji(CANCEL)) && helper.isCancellable()) && emt == CANCEL) {
						finalizeEvent(m, success);
						return;
					}

					InteractionHook hook;
					if (wrapper.getSource() instanceof ButtonInteractionEvent) {
						hook = ((ButtonInteractionEvent) wrapper.getSource()).getHook();
					} else {
						hook = null;
					}

					ThrowingConsumer<ButtonWrapper> act = lookupValue(btns, emoji);
					if (act != null) {
						act.accept(new ButtonWrapper(wrapper.getUser(), hook, m));
					}

					if (timeout != null) {
						timeout.cancel(true);
					}
					if (helper.getTimeout() > 0) {
						timeout = executor.schedule(() -> finalizeEvent(msg, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
					}

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
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit.
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pageLoader {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                   returns null the method will treat it as last page, preventing unnecessary updates.
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static ActionReference lazyPaginate(@NotNull Message msg, @NotNull ThrowingFunction<Integer, Page> pageLoader, boolean useButtons) throws ErrorResponseException, InsufficientPermissionException {
		return lazyPaginate(msg, null, pageLoader, useButtons, 0, null, null);
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
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @param time       The time before the listener automatically stop listening
	 *                   for further events (recommended: 60).
	 * @param unit       The time's {@link TimeUnit} (recommended:
	 *                   {@link TimeUnit#SECONDS}).
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static ActionReference lazyPaginate(@NotNull Message msg, @NotNull ThrowingFunction<Integer, Page> pageLoader, boolean useButtons, int time, TimeUnit unit) throws ErrorResponseException, InsufficientPermissionException {
		return lazyPaginate(msg, null, pageLoader, useButtons, time, unit, null);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pageLoader  {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                    returns null the method will treat it as last page, preventing unnecessary updates.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static ActionReference lazyPaginate(@NotNull Message msg, @NotNull ThrowingFunction<Integer, Page> pageLoader, boolean useButtons, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return lazyPaginate(msg, null, pageLoader, useButtons, 0, null, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit.
	 * You can specify how long the listener will stay active before shutting down itself after a
	 * no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pageLoader  {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                    returns null the method will treat it as last page, preventing unnecessary updates.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening.
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static ActionReference lazyPaginate(@NotNull Message msg, @NotNull ThrowingFunction<Integer, Page> pageLoader, boolean useButtons, int time, TimeUnit unit, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return lazyPaginate(msg, null, pageLoader, useButtons, time, unit, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit.
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pageCache  The {@link List} that'll hold previously visited pages (can be pre-filled or edited anytime).
	 * @param pageLoader {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                   returns null the method will treat it as last page, preventing unnecessary updates.
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static ActionReference lazyPaginate(@NotNull Message msg, List<Page> pageCache, @NotNull ThrowingFunction<Integer, Page> pageLoader, boolean useButtons) throws ErrorResponseException, InsufficientPermissionException {
		return lazyPaginate(msg, pageCache, pageLoader, useButtons, 0, null, null);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit.
	 * You can specify how long the listener will stay active before shutting down itself after a
	 * no-activity interval.
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pageCache  The {@link List} that'll hold previously visited pages (can be pre-filled or edited anytime).
	 * @param pageLoader {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                   returns null the method will treat it as last page, preventing unnecessary updates.
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @param time       The time before the listener automatically stop listening
	 *                   for further events (recommended: 60).
	 * @param unit       The time's {@link TimeUnit} (recommended:
	 *                   {@link TimeUnit#SECONDS}).
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static ActionReference lazyPaginate(@NotNull Message msg, List<Page> pageCache, @NotNull ThrowingFunction<Integer, Page> pageLoader, boolean useButtons, int time, TimeUnit unit) throws ErrorResponseException, InsufficientPermissionException {
		return lazyPaginate(msg, pageCache, pageLoader, useButtons, time, unit, null);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pageCache   The {@link List} that'll hold previously visited pages (can be pre-filled or edited anytime).
	 * @param pageLoader  {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                    returns null the method will treat it as last page, preventing unnecessary updates.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static ActionReference lazyPaginate(@NotNull Message msg, List<Page> pageCache, @NotNull ThrowingFunction<Integer, Page> pageLoader, boolean useButtons, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return lazyPaginate(msg, pageCache, pageLoader, useButtons, 0, null, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit.
	 * You can specify how long the listener will stay active before shutting down itself after a
	 * no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pageCache   The {@link List} that'll hold previously visited pages (can be pre-filled or edited anytime).
	 * @param pageLoader  {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                    returns null the method will treat it as last page, preventing unnecessary updates.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fallback to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param canInteract {@link Predicate} to determine whether the {@link User}
	 *                    that pressed the button can interact with it or not.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static ActionReference lazyPaginate(@NotNull Message msg, List<Page> pageCache, @NotNull ThrowingFunction<Integer, Page> pageLoader, boolean useButtons, int time, TimeUnit unit, Predicate<User> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return lazyPaginate(msg, new LazyPaginateHelper(pageLoader, pageCache, useButtons)
				.setTimeout(time, unit)
				.setCanInteract(canInteract)
		);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit.
	 * This versions uses a helper class to aid customization and allow reusage of configurations.
	 *
	 * @param msg    The {@link Message} sent which will be paginated.
	 * @param helper A {@link LazyPaginateHelper} holding desired lazy pagination settings.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or first page cannot be generated.
	 */
	public static ActionReference lazyPaginate(@NotNull Message msg, @NotNull LazyPaginateHelper helper) throws ErrorResponseException, InsufficientPermissionException {
		if (!isActivated()) throw new InvalidStateException();
		boolean useBtns = helper.isUsingButtons() && msg.getAuthor().getId().equals(msg.getJDA().getSelfUser().getId());
		boolean cache = helper.getContent() != null;

		if (useBtns && helper.shouldUpdate(msg)) {
			helper.apply(msg.editMessageComponents()).submit();
		} else if (!useBtns) {
			clearButtons(msg);
			clearReactions(msg);
			addReactions(msg, false, false);
		}

		return handler.addEvent(msg, new ThrowingBiConsumer<>() {
			private int p = 0;
			private ScheduledFuture<?> timeout;
			private final Consumer<Void> success = s -> {
				if (timeout != null) {
					timeout.cancel(true);
				}

				handler.removeEvent(msg);
				if (paginator.isDeleteOnCancel()) msg.delete().submit();
			};

			private final Function<Button, Button> LOWER_BOUNDARY_CHECK = b -> b.withDisabled(p == 0);

			{
				if (helper.getTimeout() > 0) {
					timeout = executor.schedule(() -> finalizeEvent(msg, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
				}
			}

			@Override
			public void acceptThrows(@NotNull User u, @NotNull PaginationEventWrapper wrapper) {
				Message m = subGet(wrapper.retrieveMessage());

				if (helper.canInteract(u)) {
					if (u.isBot() || m == null || !wrapper.getMessageId().equals(msg.getId())) return;

					Emote emt = NONE;
					if (wrapper.getContent() instanceof MessageReaction) {
						EmojiUnion reaction = ((MessageReaction) wrapper.getContent()).getEmoji();
						emt = toEmote(reaction);
					} else if (wrapper.getContent() instanceof Button) {
						Button btn = (Button) wrapper.getContent();

						if (btn.getId() != null && Emote.isNative(btn) && !btn.getId().contains(".")) {
							emt = Emote.valueOf(btn.getId().replace("*", ""));
						}
					}

					Page pg = null;
					boolean update = false;
					switch (emt) {
						case PREVIOUS:
							if (p > 0) {
								p--;
								update = true;
								pg = cache ? helper.getContent().get(p) : helper.getPageLoader().apply(p);
							}
							break;
						case NEXT:
							p++;
							update = true;

							if (cache && helper.getContent().size() > p) {
								pg = helper.getContent().get(p);
								if (pg == null) {
									pg = helper.getPageLoader().apply(p);
									if (pg == null) {
										p--;
										return;
									}
								}
							} else {
								pg = helper.getPageLoader().apply(p);
								if (pg == null) {
									p--;
									return;
								}
							}

							if (cache) helper.getContent().add(pg);
							break;
						case CANCEL:
							finalizeEvent(m, success);
							return;
					}

					if (update) {
						modifyButtons(m, pg, helper, Map.of(
								PREVIOUS.name(), LOWER_BOUNDARY_CHECK,
								SKIP_BACKWARD.name(), LOWER_BOUNDARY_CHECK,
								GOTO_FIRST.name(), LOWER_BOUNDARY_CHECK
						));
					}

					if (timeout != null) {
						timeout.cancel(true);
					}
					if (helper.getTimeout() > 0) {
						timeout = executor.schedule(() -> finalizeEvent(m, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
					}

					if (wrapper.isFromGuild() && wrapper.getSource() instanceof MessageReactionAddEvent && paginator.isRemoveOnReact()) {
						subGet(((MessageReaction) wrapper.getContent()).removeReaction(u));
					}
				}
			}
		});
	}

	/**
	 * Utility method for re-fetching a message.
	 *
	 * @param msg The {@link Message} to be reloaded.
	 * @return The updated message instance.
	 */
	public static Message reloadMessage(@NotNull Message msg) {
		return subGet(msg.getChannel().retrieveMessageById(msg.getId()), msg);
	}

	/**
	 * Utility method for submitting a {@link RestAction} and awaiting its result.
	 *
	 * @param future The {@link RestAction} to be executed.
	 * @param <T>    Return type for the {@link RestAction}.
	 * @return The {@link RestAction} result, or null should it fail.
	 */
	public static <T> T subGet(@NotNull RestAction<T> future) {
		try {
			return future.submit().get();
		} catch (InterruptedException | ExecutionException e) {
			paginator.log(PUtilsConfig.LogLevel.LEVEL_4, "Exception during future execution:", e);
			return null;
		}
	}

	/**
	 * Utility method for submitting a {@link RestAction} and awaiting its result.
	 *
	 * @param future The {@link RestAction} to be executed.
	 * @param or     Fallback value to be returned should it fail.
	 * @param <T>    Return type for the {@link RestAction}.
	 * @return The {@link RestAction} result.
	 */
	public static <T> T subGet(@NotNull RestAction<T> future, @NotNull T or) {
		try {
			return future.submit().get();
		} catch (InterruptedException | ExecutionException e) {
			paginator.log(PUtilsConfig.LogLevel.LEVEL_4, "Exception during future execution:", e);
			return or;
		}
	}

	private static Emote toEmote(EmojiUnion reaction) {
		return Emote.getByEmoji(toEmoji(reaction));
	}

	private static Emoji toEmoji(EmojiUnion reaction) {
		return Emoji.fromFormatted(reaction.getFormatted());
	}

	private static <T> T lookupValue(Map<Emoji, T> map, Emoji emoji) {
		String id;
		if (emoji instanceof CustomEmoji) {
			id = ((CustomEmoji) emoji).getId();
		} else {
			id = emoji.getFormatted();
		}

		return map.entrySet().stream()
				.filter(e -> {
					Emoji emj = e.getKey();
					if (emj instanceof CustomEmoji) {
						return ((CustomEmoji) emj).getId().equals(id);
					}

					return emj.getFormatted().equals(id);
				})
				.map(Map.Entry::getValue)
				.findFirst().orElse(null);
	}

	/**
	 * Utility method to clear all reactions of a message.
	 *
	 * @param msg The {@link Message} to have reactions/buttons removed from.
	 */
	public static void clearReactions(Message msg) {
		if (msg.getReactions().isEmpty()) return;

		try {
			if (msg.getChannel().getType().isGuild()) {
				msg.clearReactions().submit();
			} else for (MessageReaction r : msg.getReactions()) {
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
		if (msg.getButtons().isEmpty()) return;

		try {
			subGet(msg.editMessageComponents());
		} catch (InsufficientPermissionException | IllegalStateException e) {
			paginator.log(PUtilsConfig.LogLevel.LEVEL_3, "Not enough permissions to clear message reactions:", e);
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

	/**
	 * Utility method for switching pages and applying message button states.
	 *
	 * @param msg     The {@link Message} holding the buttons.
	 * @param changes {@link Map} containing desired changes, indexed by {@link Button} ID.
	 */
	public static void modifyButtons(Message msg, Page p, BaseHelper<?, ?> helper, Map<String, Function<Button, Button>> changes) {
		MessageEditAction act = msg.editMessageComponents();

		if (p.getContent() instanceof Message) {
			act = act.setContent(((Message) p.getContent()).getContentRaw());
		} else if (p.getContent() instanceof MessageEmbed) {
			act = act.setEmbeds((MessageEmbed) p.getContent());
		}

		if (p instanceof InteractPage) {
			List<LayoutComponent> rows = helper.getComponents(act);

			for (LayoutComponent lc : rows) {
				List<ItemComponent> row = lc.getComponents();
				for (int i = 0; i < row.size(); i++) {
					ItemComponent c = row.get(i);
					if (c instanceof Button) {
						Button b = (Button) c;
						if (changes.containsKey(b.getId())) {
							row.set(i, changes.get(b.getId()).apply((Button) c));
						}
					}
				}
			}
		}

		act.submit();
	}

	/**
	 * Utility method to add navigation buttons.
	 *
	 * @param msg      The {@link Message} to have reactions removed from.
	 * @param withSkip Whether to include {@link Emote#SKIP_BACKWARD} and {@link Emote#SKIP_FORWARD} buttons.
	 * @param withGoto Whether to include {@link Emote#GOTO_FIRST} and {@link Emote#GOTO_LAST} buttons.
	 */
	public static void addReactions(Message msg, boolean withSkip, boolean withGoto) {
		clearButtons(msg);
		List<RestAction<Void>> acts = new ArrayList<>();

		if (withGoto) acts.add(msg.addReaction(paginator.getEmoji(GOTO_FIRST)));
		if (withSkip) acts.add(msg.addReaction(paginator.getEmoji(SKIP_BACKWARD)));

		acts.add(msg.addReaction(paginator.getEmoji(PREVIOUS)));
		acts.add(msg.addReaction(paginator.getEmoji(CANCEL)));
		acts.add(msg.addReaction(paginator.getEmoji(NEXT)));

		if (withSkip) acts.add(msg.addReaction(paginator.getEmoji(SKIP_FORWARD)));
		if (withGoto) acts.add(msg.addReaction(paginator.getEmoji(GOTO_LAST)));

		RestAction.allOf(acts).submit();
	}
}
