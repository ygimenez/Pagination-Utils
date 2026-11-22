package com.github.ygimenez.method;

import com.github.ygimenez.exception.AlreadyActivatedException;
import com.github.ygimenez.exception.InvalidHandlerException;
import com.github.ygimenez.exception.InvalidStateException;
import com.github.ygimenez.listener.EventHandler;
import com.github.ygimenez.model.*;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.github.ygimenez.model.helper.CategorizeHelper;
import com.github.ygimenez.model.helper.LazyPaginateHelper;
import com.github.ygimenez.model.helper.PaginateHelper;
import com.github.ygimenez.type.Action;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.MessageTopLevelComponentUnion;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.ygimenez.type.Action.*;

/**
 * The main class containing all pagination-related methods, including but not limited
 * to {@link #paginate}, {@link #categorize}, {@link #buttonize} and {@link #lazyPaginate}.
 */
public abstract class Pages {
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
			((JDA) hand).addEventListener(paginator.getEvtHandler());
		} else if (hand instanceof ShardManager) {
			((ShardManager) hand).addEventListener(paginator.getEvtHandler());
		} else {
			throw new InvalidHandlerException();
		}

		Pages.paginator = paginator;
		paginator.log(PUtilsConfig.LogLevel.LEVEL_3, "Pagination Utils activated successfully");
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
			((JDA) hand).removeEventListener(paginator.getEvtHandler());
		} else if (hand instanceof ShardManager) {
			((ShardManager) hand).removeEventListener(paginator.getEvtHandler());
		}

		paginator.log(PUtilsConfig.LogLevel.LEVEL_3, "Pagination Utils deactivated successfully");
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
	 * Retrieves the current {@link Paginator}'s {@link EventHandler} instance.
	 *
	 * @return The {@link EventHandler} instance.
	 */
	public static EventHandler getHandler() {
		return paginator.getEvtHandler();
	}

	/**
	 * Retrieves the current {@link Paginator}'s {@link TaskScheduler} instance.
	 *
	 * @return The {@link TaskScheduler} instance.
	 */
	public static TaskScheduler getScheduler() {
		return paginator.getScheduler();
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pages      The pages to be shown. The order of the {@link List} will
	 *                   define the order of the pages.
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
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
	 * how long the listener will stay active before shutting itself down after a
	 * no-activity interval.
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pages      The pages to be shown. The order of the {@link List} will
	 *                   define the order of the pages.
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @param time       The time before the listener automatically stop listening
	 *                   for further events (recommended: 60).
	 * @param unit       The time's {@link TimeUnit} (recommended:
	 *                   {@link TimeUnit#SECONDS}).
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
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
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param canInteract {@link Predicate} to determine whether the interaction is to be allowed.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, 0, null, 0, false, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You can specify
	 * how long the listener will stay active before shutting itself down after a
	 * no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param canInteract {@link Predicate} to determine whether the interaction is to be allowed.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int time, TimeUnit unit, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, time, unit, 0, false, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param fastForward Whether the {@link Action#GOTO_FIRST} and {@link Action#GOTO_LAST} buttons should be shown.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
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
	 * how long the listener will stay active before shutting itself down after a
	 * no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param fastForward Whether the {@link Action#GOTO_FIRST} and {@link Action#GOTO_LAST} buttons should be shown.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
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
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param fastForward Whether the {@link Action#GOTO_FIRST} and {@link Action#GOTO_LAST} buttons should be shown.
	 * @param canInteract {@link Predicate} to determine whether the interaction is to be allowed.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, boolean fastForward, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, 0, null, 0, fastForward, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You can specify
	 * how long the listener will stay active before shutting itself down after a
	 * no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param fastForward Whether the {@link Action#GOTO_FIRST} and {@link Action#GOTO_LAST} buttons should be shown.
	 * @param canInteract {@link Predicate} to determine whether the interaction is to be allowed.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int time, TimeUnit unit, boolean fastForward, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, time, unit, 0, fastForward, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pages      The pages to be shown. The order of the {@link List} will
	 *                   define the order of the pages.
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @param skipAmount The number of pages to be skipped when clicking {@link Action#SKIP_BACKWARD}
	 *                   and {@link Action#SKIP_FORWARD} buttons.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
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
	 * how long the listener will stay active before shutting itself down after a
	 * no-activity interval.
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pages      The pages to be shown. The order of the {@link List} will
	 *                   define the order of the pages.
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @param time       The time before the listener automatically stop listening
	 *                   for further events (recommended: 60).
	 * @param unit       The time's {@link TimeUnit} (recommended:
	 *                   {@link TimeUnit#SECONDS}).
	 * @param skipAmount The number of pages to be skipped when clicking {@link Action#SKIP_BACKWARD}
	 *                   and {@link Action#SKIP_FORWARD} buttons.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
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
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param skipAmount  The number of pages to be skipped when clicking {@link Action#SKIP_BACKWARD}
	 *                    and {@link Action#SKIP_FORWARD} buttons.
	 * @param canInteract {@link Predicate} to determine whether the interaction is to be allowed.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int skipAmount, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, 0, null, skipAmount, false, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You can specify
	 * how long the listener will stay active before shutting itself down after a
	 * no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param skipAmount  The number of pages to be skipped when clicking {@link Action#SKIP_BACKWARD}
	 *                    and {@link Action#SKIP_FORWARD} buttons.
	 * @param canInteract {@link Predicate} to determine whether the interaction is to be allowed.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int time, TimeUnit unit, int skipAmount, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, time, unit, skipAmount, false, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param skipAmount  The number of pages to be skipped when clicking {@link Action#SKIP_BACKWARD}
	 *                    and {@link Action#SKIP_FORWARD} buttons.
	 * @param fastForward Whether the {@link Action#GOTO_FIRST} and {@link Action#GOTO_LAST} buttons should be shown.
	 * @param canInteract {@link Predicate} to determine whether the interaction is to be allowed.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int skipAmount, boolean fastForward, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, pages, useButtons, 0, null, skipAmount, fastForward, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. You can specify
	 * how long the listener will stay active before shutting itself down after a
	 * no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param skipAmount  The number of pages to be skipped when clicking {@link Action#SKIP_BACKWARD}
	 *                    and {@link Action#SKIP_FORWARD} buttons.
	 * @param fastForward Whether the {@link Action#GOTO_FIRST} and {@link Action#GOTO_LAST} buttons should be shown.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
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
	 * how long the listener will stay active before shutting itself down after a
	 * no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pages       The pages to be shown. The order of the {@link List} will
	 *                    define the order of the pages.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param skipAmount  The number of pages to be skipped when clicking {@link Action#SKIP_BACKWARD}
	 *                    and {@link Action#SKIP_FORWARD} buttons.
	 * @param fastForward Whether the {@link Action#GOTO_FIRST} and {@link Action#GOTO_LAST} buttons should be shown.
	 * @param canInteract {@link Predicate} to determine whether the interaction is to be allowed.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the page list is empty.
	 */
	public static ActionReference paginate(@NotNull Message msg, @NotNull List<Page> pages, boolean useButtons, int time, TimeUnit unit, int skipAmount, boolean fastForward, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return paginate(msg, new PaginateHelper(pages, useButtons)
				.setTimeout(time, unit)
				.setSkipAmount(skipAmount)
				.setFastForward(fastForward)
				.setCanInteract(canInteract)
		);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will navigate through a given {@link List} of pages. This version uses a helper class
	 * to aid customization and allow usage of configurations.
	 *
	 * @param msg    The {@link Message} sent which will be paginated.
	 * @param helper A {@link PaginateHelper} holding desired pagination settings.
	 * @return An {@link ActionReference} pointing to this action. This is useful if you need to track whether an event
	 * is still being processed or was already removed (i.e., garbage collected).
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
		if (helper.hasActions()) return paginateWithActions(msg, helper, useBtns);
		List<Page> pgs = Collections.unmodifiableList(helper.getContent());

		if (useBtns && helper.shouldUpdate(msg)) {
			helper.apply(msg.editMessageComponents()).submit();
		} else if (!useBtns) {
			clearButtons(msg);
			clearReactions(msg);
			addReactions(msg, helper.getSkipAmount() > 1, helper.isFastForward());
		}

		String evt = getHandler().getEventId(msg);
		return getHandler().addEvent(evt, new EventData<>(helper, new ThrowingBiConsumer<>() {
			private final int maxP = pgs.size() - 1;
			private int p = 0;
			private ScheduledFuture<?> timeout;
			private final Consumer<Void> success = s -> {
				if (timeout != null) {
					timeout.cancel(true);
				}
			};

			private final Function<Button, Button> LOWER_BOUNDARY_CHECK = b -> b.withDisabled(p == 0);
			private final Function<Button, Button> UPPER_BOUNDARY_CHECK = b -> b.withDisabled(p == maxP);

			{
				if (helper.getTimeout() > 0) {
					timeout = getScheduler().schedule(evt, () -> finalizeEvent(msg, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
				}
			}

			@Override
			public void acceptThrows(@NotNull User u, @NotNull PaginationEventWrapper wrapper) {
				Message msg = wrapper.retrieveMessage();
				if (msg == null || !wrapper.getMessageId().equals(msg.getId())) return;

				Action emt = NONE;
				if (wrapper.getContent() instanceof MessageReaction) {
					EmojiUnion reaction = ((MessageReaction) wrapper.getContent()).getEmoji();
					emt = toEmote(reaction);
				} else if (wrapper.getContent() instanceof Button) {
					Button btn = (Button) wrapper.getContent();

					if (btn.getCustomId() != null && Action.isNative(btn)) {
						emt = Action.valueOf(ButtonId.ID_PATTERN.split(btn.getCustomId())[0]);
					}
				}

				Page pg;
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
						finalizeEvent(msg, wrapper.getHook(), success);
						return;
				}

				if (update) {
					pg = pgs.get(p);
					modifyButtons(msg, pg, Map.of(
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
					timeout = getScheduler().schedule(evt, () -> finalizeEvent(msg, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
				}

				if (wrapper.isFromGuild() && wrapper.getSource() instanceof MessageReactionAddEvent && paginator.isRemoveOnReact()) {
					((MessageReaction) wrapper.getContent()).removeReaction(u).submit();
				}
			}
		}));
	}

	private static ActionReference paginateWithActions(Message msg, PaginateHelper helper, boolean useButtons) {
		PaginateHelper session = helper.clone();
		session.validateActions(useButtons);
		CombinedPagination action = new CombinedPagination(msg, session, useButtons);
		ActionReference reference = getHandler().addEvent(action.eventId, new EventData<>(session, action));
		try {
			action.start();
		} catch (RuntimeException error) {
			if (action.isCurrent()) getHandler().removeEvent(action.eventId);
			throw error;
		}
		return reference;
	}

	/** One registration owns navigation, custom actions and their shared lifetime. */
	private static final class CombinedPagination implements ThrowingBiConsumer<User, PaginationEventWrapper> {
		private final Message original;
		private final PaginateHelper helper;
		private final boolean useButtons;
		private final String eventId;
		private final Map<String, Action> navigationButtons = new HashMap<>();
		private final Map<String, ThrowingBiConsumer<ButtonWrapper, Integer>> actionButtons = new HashMap<>();
		private final Map<Emoji, Action> navigationReactions = new LinkedHashMap<>();
		private final Map<Emoji, ThrowingBiConsumer<ButtonWrapper, Integer>> actionReactions = new LinkedHashMap<>();
		private int page;
		private boolean finished;
		private boolean rendering;
		private ScheduledFuture<?> timeout;
		private long timeoutVersion;

		private CombinedPagination(Message original, PaginateHelper helper, boolean useButtons) {
			this.original = original;
			this.helper = helper;
			this.useButtons = useButtons;
			this.eventId = getHandler().getEventId(original);
			if (helper.isFastForward()) navigationReactions.put(paginator.getEmoji(GOTO_FIRST), GOTO_FIRST);
			if (helper.getSkipAmount() > 1) navigationReactions.put(paginator.getEmoji(SKIP_BACKWARD), SKIP_BACKWARD);
			navigationReactions.put(paginator.getEmoji(PREVIOUS), PREVIOUS);
			if (helper.isCancellable()) navigationReactions.put(paginator.getEmoji(CANCEL), CANCEL);
			navigationReactions.put(paginator.getEmoji(NEXT), NEXT);
			if (helper.getSkipAmount() > 1) navigationReactions.put(paginator.getEmoji(SKIP_FORWARD), SKIP_FORWARD);
			if (helper.isFastForward()) navigationReactions.put(paginator.getEmoji(GOTO_LAST), GOTO_LAST);
		}

		private synchronized void start() {
			if (!useButtons) clearButtons(original);
			render(original, 0, false);
		}

		private boolean isCurrent() {
			if (finished || !isActivated()) return false;
			EventData<?, ?> data = getHandler().getEventMap().get(eventId);
			return data != null && data.getAction() == this;
		}

		private void refreshTimeout() {
			long version = ++timeoutVersion;
			if (timeout != null) timeout.cancel(false);
			if (helper.getTimeout() > 0 && isCurrent()) {
				timeout = getScheduler().schedule(eventId, () -> expire(version),
						helper.getTimeout(), TimeUnit.MILLISECONDS);
			}
		}

		private synchronized void expire(long version) {
			if (version == timeoutVersion) finish(original, null);
		}

		private synchronized void finish(Message message, InteractionHook hook) {
			if (!isCurrent()) return;
			finished = true;
			if (timeout != null) timeout.cancel(false);
			finalizeEvent(message, hook, ignored -> { });
		}

		@Override
		public synchronized void acceptThrows(User user, PaginationEventWrapper wrapper) {
			if (!isCurrent() || rendering || !original.getId().equals(wrapper.getMessageId())) return;
			Action navigation;
			ThrowingBiConsumer<ButtonWrapper, Integer> callback;
			Button button = null;
			if (useButtons && wrapper.getContent() instanceof Button) {
				button = (Button) wrapper.getContent();
				navigation = navigationButtons.get(button.getCustomId());
				callback = actionButtons.get(button.getCustomId());
			} else if (!useButtons && wrapper.getContent() instanceof MessageReaction) {
				Emoji emoji = toEmoji(((MessageReaction) wrapper.getContent()).getEmoji());
				navigation = navigationReactions.get(emoji);
				callback = actionReactions.get(emoji);
			} else {
				return;
			}
			// Full component IDs are scoped to the last rendering. Unknown or obsolete input does nothing.
			if (navigation == null && callback == null) return;
			Message message = wrapper.retrieveMessage();
			if (message == null) return;
			if (navigation == CANCEL && helper.isCancellable()) {
				finish(message, wrapper.getHook());
				return;
			}

			int targetPage = page;
			int last = helper.getContent().size() - 1;
			if (navigation != null) {
				switch (navigation) {
					case PREVIOUS: targetPage = Math.max(0, page - 1); break;
					case NEXT: targetPage = Math.min(last, page + 1); break;
					case SKIP_BACKWARD: targetPage = Math.max(0, page - helper.getSkipAmount()); break;
					case SKIP_FORWARD: targetPage = (int) Math.min(last, (long) page + helper.getSkipAmount()); break;
					case GOTO_FIRST: targetPage = 0; break;
					case GOTO_LAST: targetPage = last; break;
					default: return;
				}
			}
			if (targetPage != page) {
				render(message, targetPage, true);
				removeUserReaction(user, wrapper);
				return;
			}
			try {
				if (callback != null) {
					callback.acceptThrows(new ButtonWrapper(user, wrapper.getHook(), button,
							helper.getDropdownValues(), message), page);
				}
			} finally {
				refreshTimeout();
				removeUserReaction(user, wrapper);
			}
		}

		private void removeUserReaction(User user, PaginationEventWrapper wrapper) {
			Paginator currentPaginator = getPaginator();
			if (currentPaginator != null && wrapper.isFromGuild() && wrapper.getSource() instanceof MessageReactionAddEvent
					&& currentPaginator.isRemoveOnReact()) {
				((MessageReaction) wrapper.getContent()).removeReaction(user).submit();
			}
		}

		private void render(Message message, int targetPage, boolean updateContent) {
			Map<ButtonId<?>, ThrowingBiConsumer<ButtonWrapper, Integer>> actions = helper.getActions(targetPage);
			Map<String, Action> nextNavigation = new HashMap<>();
			Map<String, ThrowingBiConsumer<ButtonWrapper, Integer>> nextButtons = new HashMap<>();
			Map<Emoji, ThrowingBiConsumer<ButtonWrapper, Integer>> nextReactions = new LinkedHashMap<>();
			MessageEditAction edit = null;
			if (useButtons) {
				List<MessageTopLevelComponent> components = helper.getComponents(targetPage);
				Iterator<ThrowingBiConsumer<ButtonWrapper, Integer>> callbacks = actions.values().iterator();
				for (MessageTopLevelComponent component : components) {
					for (Button button : ((ActionRow) component).getButtons()) {
						if (button.getCustomId().startsWith("PU_ACTION_")) {
							nextButtons.put(button.getCustomId(), callbacks.next());
						} else if (!button.isDisabled() && Action.isNative(button)) {
							nextNavigation.put(button.getCustomId(), Action.valueOf(ButtonId.ID_PATTERN.split(button.getCustomId())[0]));
						}
					}
				}
				edit = updateContent ? editPage(message, targetPage) : message.editMessageComponents();
				edit.setComponents(components);
			} else {
				for (Map.Entry<ButtonId<?>, ThrowingBiConsumer<ButtonWrapper, Integer>> entry : actions.entrySet()) {
					nextReactions.put(((EmojiId) entry.getKey()).getContent(), entry.getValue());
				}
				if (updateContent) edit = editPage(message, targetPage);
			}
			rendering = true;
			// A valid navigation request suspends expiry until its REST result arrives.
			// A timer already waiting for this monitor must not remove the pending session.
			timeoutVersion++;
			if (timeout != null) {
				timeout.cancel(false);
				timeout = null;
			}
			try {
				CompletableFuture<Message> request = edit == null ? CompletableFuture.completedFuture(message) : edit.submit();
				request.whenComplete((updated, error) -> {
					synchronized (this) {
						if (!isCurrent()) return;
						if (error != null) {
							renderFailed(error, updateContent);
							return;
						}
						page = targetPage;
						navigationButtons.clear();
						navigationButtons.putAll(nextNavigation);
						actionButtons.clear();
						actionButtons.putAll(nextButtons);
						Set<Emoji> previous = new LinkedHashSet<>(actionReactions.keySet());
						actionReactions.clear();
						actionReactions.putAll(nextReactions);
						try {
							if (!useButtons) syncReactions(message, previous);
						} catch (RuntimeException reactionError) {
							paginator.log(PUtilsConfig.LogLevel.LEVEL_1, "Could not update pagination reactions", reactionError);
						} finally {
							rendering = false;
							refreshTimeout();
						}
					}
				});
			} catch (RuntimeException error) {
				renderFailed(error, updateContent);
			}
		}

		private void renderFailed(Throwable error, boolean updateContent) {
			if (!isCurrent()) return;
			rendering = false;
			paginator.log(PUtilsConfig.LogLevel.LEVEL_1, "Could not render pagination controls", error);
			if (updateContent) {
				// The old page and full component IDs remain valid, so the user can retry.
				refreshTimeout();
			} else {
				finished = true;
				if (timeout != null) timeout.cancel(false);
				getHandler().removeEvent(eventId);
			}
		}

		private MessageEditAction editPage(Message message, int pageIndex) {
			Object content = helper.getContent().get(pageIndex).getContent();
			if (content instanceof String) return message.editMessage((String) content);
			if (content instanceof MessageEmbed) return message.editMessageEmbeds((MessageEmbed) content);
			return message.editMessageEmbeds(((EmbedCluster) content).getEmbeds());
		}

		private void syncReactions(Message message, Set<Emoji> previous) {
			Set<Emoji> desired = new LinkedHashSet<>(navigationReactions.keySet());
			desired.addAll(actionReactions.keySet());
			boolean canManage = message.isFromGuild() && message.getGuild().getSelfMember()
					.hasPermission(message.getGuildChannel(), Permission.MESSAGE_MANAGE);
			for (MessageReaction reaction : message.getReactions()) {
				if (canManage || reaction.isSelf()) previous.add(toEmoji(reaction.getEmoji()));
			}
			previous.removeAll(desired);
			for (Emoji obsolete : previous) {
				try {
					// Await removal before adding controls, preserving reaction slots at Discord's limit.
					subGet(canManage ? message.clearReactions(obsolete) : message.removeReaction(obsolete));
				} catch (InsufficientPermissionException error) {
					// Removing only the bot's own reaction does not require MESSAGE_MANAGE.
					subGet(message.removeReaction(obsolete));
				}
			}
			for (Emoji emoji : desired) message.addReaction(emoji).submit();
		}
	}

	/**
	 * Adds menu-like buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will browse through a given {@link Map} of pages. You may only specify
	 * one {@link Page} per button, adding another button with an existing Unicode
	 * will overwrite the current button's {@link Page}.
	 *
	 * @param msg        The {@link Message} sent which will be categorized.
	 * @param categories The categories to be shown. The categories are defined by
	 *                   a {@link Map} containing emoji unicodes or emote ids as keys and
	 *                   {@link Pages} as values.
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
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
	public static ActionReference categorize(@NotNull Message msg, @NotNull Map<ButtonId<?>, Page> categories, boolean useButtons) throws ErrorResponseException, InsufficientPermissionException {
		return categorize(msg, categories, useButtons, 0, null, null);
	}

	/**
	 * Adds menu-like buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will browse through a given {@link Map} of pages. You may only specify
	 * one {@link Page} per button, adding another button with an existing Unicode
	 * will overwrite the current button's {@link Page}. You can specify how long
	 * the listener will stay active before shutting itself down after a no-activity
	 * interval.
	 *
	 * @param msg        The {@link Message} sent which will be categorized.
	 * @param categories The categories to be shown. The categories are defined by
	 *                   a {@link Map} containing emoji unicodes or emote ids as keys and
	 *                   {@link Pages} as values.
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
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
	public static ActionReference categorize(@NotNull Message msg, @NotNull Map<ButtonId<?>, Page> categories, boolean useButtons, int time, TimeUnit unit) throws ErrorResponseException, InsufficientPermissionException {
		return categorize(msg, categories, useButtons, time, unit, null);
	}

	/**
	 * Adds menu-like buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will browse through a given {@link Map} of pages. You may only specify
	 * one {@link Page} per button, adding another button with an existing Unicode
	 * will overwrite the current button's {@link Page}.
	 *
	 * @param msg         The {@link Message} sent which will be categorized.
	 * @param categories  The categories to be shown. The categories are defined by
	 *                    a {@link Map} containing emoji unicodes or emote ids as keys and
	 *                    {@link Pages} as values.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param canInteract {@link Predicate} to determine whether the interaction is to be allowed.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static ActionReference categorize(@NotNull Message msg, @NotNull Map<ButtonId<?>, Page> categories, boolean useButtons, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return categorize(msg, categories, useButtons, 0, null, canInteract);
	}

	/**
	 * Adds menu-like buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will browse through a given {@link Map} of pages. You may only specify
	 * one {@link Page} per button, adding another button with an existing Unicode
	 * will overwrite the current button's {@link Page}. You can specify how long
	 * the listener will stay active before shutting itself down after a no-activity
	 * interval.
	 *
	 * @param msg         The {@link Message} sent which will be categorized.
	 * @param categories  The categories to be shown. The categories are defined by
	 *                    a {@link Map} containing emoji unicodes or emote ids as keys and
	 *                    {@link Pages} as values.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param canInteract {@link Predicate} to determine whether the interaction is to be allowed.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static ActionReference categorize(@NotNull Message msg, @NotNull Map<ButtonId<?>, Page> categories, boolean useButtons, int time, TimeUnit unit, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return categorize(msg, new CategorizeHelper(categories, useButtons)
				.setTimeout(time, unit)
				.setCanInteract(canInteract)
		);
	}

	/**
	 * Adds menu-like buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will browse through a given {@link Map} of pages. You may only specify
	 * one {@link Page} per button, adding another button with an existing Unicode
	 * will overwrite the current button's {@link Page}. This version uses a helper class
	 * to aid customization and allow usage of configurations.
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

		Map<ButtonId<?>, Page> cats = Collections.unmodifiableMap(helper.getContent());

		if (useBtns && helper.shouldUpdate(msg)) {
			helper.apply(msg.editMessageComponents()).submit();
		} else if (!useBtns) {
			clearButtons(msg);
			clearReactions(msg);

			for (ButtonId<?> k : cats.keySet()) {
				if (k instanceof EmojiId) {
					msg.addReaction(((EmojiId) k).getContent()).submit();
				}
			}

			msg.addReaction(paginator.getEmoji(CANCEL)).submit();
		}

		String evt = getHandler().getEventId(msg);
		return getHandler().addEvent(evt, new EventData<>(helper, new ThrowingBiConsumer<>() {
			private ButtonId<?> currCat = null;
			private ScheduledFuture<?> timeout;
			private final Consumer<Void> success = s -> {
				if (timeout != null) {
					timeout.cancel(true);
				}
			};

			{
				if (helper.getTimeout() > 0) {
					timeout = getScheduler().schedule(evt, () -> finalizeEvent(msg, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
				}
			}

			@Override
			public void acceptThrows(@NotNull User u, @NotNull PaginationEventWrapper wrapper) {
				Message m = wrapper.retrieveMessage();
				if (m == null || !wrapper.getMessageId().equals(msg.getId())) return;

				ButtonId<?> id = null;
				Action emt = NONE;
				if (wrapper.getContent() instanceof MessageReaction) {
					EmojiUnion reaction = ((MessageReaction) wrapper.getContent()).getEmoji();
					id = new EmojiId(toEmoji(reaction));
					emt = toEmote(reaction);
				} else if (wrapper.getContent() instanceof Button) {
					Button btn = (Button) wrapper.getContent();

					String customId = "";
					String[] idParts = ButtonId.ID_PATTERN.split(btn.getCustomId());
					if (idParts.length == 2) {
						customId = idParts[0];
					}

					if (btn.getEmoji() == null) {
						id = new TextId(customId, btn.getLabel(), btn.getStyle());
					} else {
						id = new EmojiId(customId, btn.getEmoji(), btn.getLabel(), btn.getStyle());
					}

					if (btn.getCustomId() != null && Action.isNative(btn)) {
						emt = Action.valueOf(ButtonId.ID_PATTERN.split(btn.getCustomId())[0]);
					}
				}

				if (emt == CANCEL) {
					finalizeEvent(m, wrapper.getHook(), success);
					return;
				} else if (id != null && !Objects.equals(id, currCat)) {
					Page pg = cats.get(id);
					if (pg != null) {
						if (currCat != null) {
							modifyButtons(m, pg, Map.of(
									currCat.getId(), Button::asEnabled,
									(currCat = id).getId(), Button::asDisabled
							));
						} else {
							modifyButtons(m, pg, Map.of((currCat = id).getId(), Button::asDisabled));
						}
					}
				}

				if (timeout != null) {
					timeout.cancel(true);
				}
				if (helper.getTimeout() > 0) {
					timeout = getScheduler().schedule(evt, () -> finalizeEvent(msg, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
				}

				if (wrapper.isFromGuild() && wrapper.getSource() instanceof MessageReactionAddEvent && paginator.isRemoveOnReact()) {
					((MessageReaction) wrapper.getContent()).removeReaction(u).submit();
				}
			}
		}));
	}

	/**
	 * Adds buttons to the specified {@link Message}/{@link MessageEmbed}, with each
	 * executing a specific task on click. You may only specify one {@link Runnable}
	 * per button, adding another button with an existing Unicode will overwrite the
	 * current button's {@link Runnable}.
	 *
	 * @param msg              The {@link Message} sent which will be buttoned.
	 * @param buttons          The buttons to be shown. The buttons are defined by a
	 *                         Map containing emoji unicodes or emote ids as keys and
	 *                         {@link ThrowingTriConsumer}&lt;{@link Member}, {@link Message}, {@link InteractionHook}&gt;
	 *                         containing desired behavior as value.
	 * @param useButtons       Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                         {@link Message} was not sent by the bot.
	 * @param showCancelButton Should the {@link Action#CANCEL} button be created automatically?
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated.
	 */
	public static ActionReference buttonize(@NotNull Message msg, @NotNull Map<ButtonId<?>, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton) throws ErrorResponseException, InsufficientPermissionException {
		return buttonize(msg, buttons, useButtons, showCancelButton, 0, null, null, null);
	}

	/**
	 * Adds buttons to the specified {@link Message}/{@link MessageEmbed}, with each
	 * executing a specific task on click. You may only specify one {@link Runnable}
	 * per button, adding another button with an existing Unicode will overwrite the
	 * current button's {@link Runnable}. You can specify the time in which the
	 * listener will automatically stop itself after a no-activity interval.
	 *
	 * @param msg              The {@link Message} sent which will be buttoned.
	 * @param buttons          The buttons to be shown. The buttons are defined by a
	 *                         Map containing emoji unicodes or emote ids as keys and
	 *                         {@link ThrowingTriConsumer}&lt;{@link Member}, {@link Message}, {@link InteractionHook}&gt;
	 *                         containing desired behavior as value.
	 * @param useButtons       Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                         {@link Message} was not sent by the bot.
	 * @param showCancelButton Should the {@link Action#CANCEL} button be created automatically?
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
	public static ActionReference buttonize(@NotNull Message msg, @NotNull Map<ButtonId<?>, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton, int time, TimeUnit unit) throws ErrorResponseException, InsufficientPermissionException {
		return buttonize(msg, buttons, useButtons, showCancelButton, time, unit, null, null);
	}

	/**
	 * Adds buttons to the specified {@link Message}/{@link MessageEmbed}, with each
	 * executing a specific task on click. You may only specify one {@link Runnable}
	 * per button, adding another button with an existing Unicode will overwrite the
	 * current button's {@link Runnable}.
	 *
	 * @param msg              The {@link Message} sent which will be buttoned.
	 * @param buttons          The buttons to be shown. The buttons are defined by a
	 *                         Map containing emoji unicodes or emote ids as keys and
	 *                         {@link ThrowingTriConsumer}&lt;{@link Member}, {@link Message}, {@link InteractionHook}&gt;
	 *                         containing desired behavior as value.
	 * @param useButtons       Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                         {@link Message} was not sent by the bot.
	 * @param showCancelButton Should the {@link Action#CANCEL} button be created automatically?
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
	public static ActionReference buttonize(@NotNull Message msg, @NotNull Map<ButtonId<?>, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return buttonize(msg, buttons, useButtons, showCancelButton, 0, null, canInteract, null);
	}

	/**
	 * Adds buttons to the specified {@link Message}/{@link MessageEmbed}, with each
	 * executing a specific task on click. You may only specify one {@link Runnable}
	 * per button, adding another button with an existing Unicode will overwrite the
	 * current button's {@link Runnable}. You can specify the time in which the
	 * listener will automatically stop itself after a no-activity interval.
	 *
	 * @param msg              The {@link Message} sent which will be buttoned.
	 * @param buttons          The buttons to be shown. The buttons are defined by a
	 *                         Map containing emoji unicodes or emote ids as keys and
	 *                         {@link ThrowingTriConsumer}&lt;{@link Member}, {@link Message}, {@link InteractionHook}&gt;
	 *                         containing desired behavior as value.
	 * @param useButtons       Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                         {@link Message} was not sent by the bot.
	 * @param showCancelButton Should the {@link Action#CANCEL} button be created automatically?
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
	public static ActionReference buttonize(@NotNull Message msg, @NotNull Map<ButtonId<?>, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton, int time, TimeUnit unit, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return buttonize(msg, buttons, useButtons, showCancelButton, time, unit, canInteract, null);
	}

	/**
	 * Adds buttons to the specified {@link Message}/{@link MessageEmbed}, with each
	 * executing a specific task on click. You may only specify one {@link Runnable}
	 * per button, adding another button with an existing Unicode will overwrite the
	 * current button's {@link Runnable}.
	 *
	 * @param msg              The {@link Message} sent which will be buttoned.
	 * @param buttons          The buttons to be shown. The buttons are defined by a
	 *                         Map containing emoji unicodes or emote ids as keys and
	 *                         {@link ThrowingTriConsumer}&lt;{@link Member}, {@link Message}, {@link InteractionHook}&gt;
	 *                         containing desired behavior as value.
	 * @param useButtons       Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                         {@link Message} was not sent by the bot.
	 * @param showCancelButton Should the {@link Action#CANCEL} button be created automatically?
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
	public static ActionReference buttonize(@NotNull Message msg, @NotNull Map<ButtonId<?>, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean showCancelButton, Predicate<InteractionData> canInteract, Consumer<Message> onCancel) throws ErrorResponseException, InsufficientPermissionException {
		return buttonize(msg, buttons, useButtons, showCancelButton, 0, null, canInteract, onCancel);
	}

	/**
	 * Adds buttons to the specified {@link Message}/{@link MessageEmbed}, with each
	 * executing a specific task on click. You may only specify one {@link Runnable}
	 * per button, adding another button with an existing Unicode will overwrite the
	 * current button's {@link Runnable}. You can specify the time in which the
	 * listener will automatically stop itself after a no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be buttoned.
	 * @param buttons     The buttons to be shown. The buttons are defined by a
	 *                    Map containing emoji unicodes or emote ids as keys and
	 *                    {@link ThrowingTriConsumer}&lt;{@link Member}, {@link Message}, {@link InteractionHook}&gt;
	 *                    containing desired behavior as value.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param cancellable Should the {@link Action#CANCEL} button be created automatically?
	 * @param time        The time before the listener automatically stop
	 *                    listening for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param canInteract {@link Predicate} to determine whether the interaction is to be allowed.
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
	public static ActionReference buttonize(@NotNull Message msg, @NotNull Map<ButtonId<?>, ThrowingConsumer<ButtonWrapper>> buttons, boolean useButtons, boolean cancellable, int time, TimeUnit unit, Predicate<InteractionData> canInteract, Consumer<Message> onCancel) throws ErrorResponseException, InsufficientPermissionException {
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
	 * per button, adding another button with an existing Unicode will overwrite the
	 * current button's {@link Runnable}. This version uses a helper class
	 * to aid customization and allow usage of configurations.
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

		Map<ButtonId<?>, ThrowingConsumer<ButtonWrapper>> btns = Collections.unmodifiableMap(helper.getContent());

		if (useBtns && helper.shouldUpdate(msg)) {
			helper.apply(msg.editMessageComponents()).submit();
		} else if (!useBtns) {
			clearButtons(msg);
			clearReactions(msg);

			for (ButtonId<?> k : btns.keySet()) {
				if (k instanceof EmojiId) {
					msg.addReaction(((EmojiId) k).getContent()).submit();
				}
			}

			boolean hasCancel = btns.keySet().stream().anyMatch(b -> Objects.equals(b.getContent(), Pages.getPaginator().getEmoji(CANCEL)));
			if (!hasCancel && helper.isCancellable()) {
				msg.addReaction(paginator.getEmoji(CANCEL)).submit();
			}
		}

		String evt = getHandler().getEventId(msg);
		return getHandler().addEvent(evt, new EventData<>(helper, new ThrowingBiConsumer<>() {
			private ScheduledFuture<?> timeout;
			private final Consumer<Void> success = s -> {
				if (timeout != null) {
					timeout.cancel(true);
				}

				if (helper.getOnFinalization() != null) helper.getOnFinalization().accept(msg);
			};

			{
				if (helper.getTimeout() > 0) {
					timeout = getScheduler().schedule(evt, () -> finalizeEvent(msg, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
				}
			}

			@Override
			public void acceptThrows(@NotNull User u, @NotNull PaginationEventWrapper wrapper) {
				Message m = wrapper.retrieveMessage();
				if (m == null || !wrapper.getMessageId().equals(msg.getId())) return;

				ButtonId<?> id = null;
				Action emt = NONE;
				if (wrapper.getContent() instanceof MessageReaction) {
					EmojiUnion reaction = ((MessageReaction) wrapper.getContent()).getEmoji();
					id = new EmojiId(toEmoji(reaction));
					emt = toEmote(reaction);
				} else if (wrapper.getContent() instanceof Button) {
					Button btn = (Button) wrapper.getContent();

					String customId = "";
					String[] idParts = ButtonId.ID_PATTERN.split(btn.getCustomId());
					if (idParts.length == 2) {
						customId = idParts[0];
					}

					if (btn.getEmoji() == null) {
						id = new TextId(customId, btn.getLabel(), btn.getStyle());
					} else {
						id = new EmojiId(customId, btn.getEmoji(), btn.getLabel(), btn.getStyle());
					}

					if (btn.getCustomId() != null && Action.isNative(btn)) {
						emt = Action.valueOf(ButtonId.ID_PATTERN.split(btn.getCustomId())[0]);
					}
				}

				boolean hasCancel = btns.keySet().stream().anyMatch(b -> Objects.equals(b.getContent(), Pages.getPaginator().getEmoji(CANCEL)));
				if ((!hasCancel && helper.isCancellable()) && emt == CANCEL) {
					finalizeEvent(m, wrapper.getHook(), success);
					return;
				}

				Button button;
				InteractionHook hook;
				if (wrapper.getSource() instanceof ButtonInteractionEvent) {
					button = (Button) wrapper.getContent();
					hook = ((ButtonInteractionEvent) wrapper.getSource()).getHook();
				} else {
					button = null;
					hook = null;
				}

				ThrowingConsumer<ButtonWrapper> act = btns.get(id);
				if (act != null) {
					act.accept(new ButtonWrapper(wrapper.getUser(), hook, button, helper.getDropdownValues(), m));
				}

				if (timeout != null) {
					timeout.cancel(true);
				}
				if (helper.getTimeout() > 0) {
					timeout = getScheduler().schedule(evt, () -> finalizeEvent(msg, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
				}

				if (wrapper.isFromGuild() && wrapper.getSource() instanceof MessageReactionAddEvent && paginator.isRemoveOnReact()) {
					((MessageReaction) wrapper.getContent()).removeReaction(u).submit();
				}
			}
		}));
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit.
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pageLoader {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                   returns null, the method will treat it as the last page, preventing unnecessary updates.
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the first page cannot be generated.
	 */
	public static ActionReference lazyPaginate(@NotNull Message msg, @NotNull ThrowingFunction<Integer, Page> pageLoader, boolean useButtons) throws ErrorResponseException, InsufficientPermissionException {
		return lazyPaginate(msg, null, pageLoader, useButtons, 0, null, null);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit
	 * You can specify how long the listener will stay active before shutting itself down after a
	 * no-activity interval.
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pageLoader {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                   returns null, the method will treat it as the last page, preventing unnecessary updates.
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
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
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the first page cannot be generated.
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
	 *                    returns null, the method will treat it as the last page, preventing unnecessary updates.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param canInteract {@link Predicate} to determine whether the interaction is to be allowed.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the first page cannot be generated.
	 */
	public static ActionReference lazyPaginate(@NotNull Message msg, @NotNull ThrowingFunction<Integer, Page> pageLoader, boolean useButtons, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return lazyPaginate(msg, null, pageLoader, useButtons, 0, null, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit.
	 * You can specify how long the listener will stay active before shutting itself down after a
	 * no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pageLoader  {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                    returns null, the method will treat it as the last page, preventing unnecessary updates.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param canInteract {@link Predicate} to determine whether the interaction is to be allowed.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the first page cannot be generated.
	 */
	public static ActionReference lazyPaginate(@NotNull Message msg, @NotNull ThrowingFunction<Integer, Page> pageLoader, boolean useButtons, int time, TimeUnit unit, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
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
	 *                   returns null, the method will treat it as the last page, preventing unnecessary updates.
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                   {@link Message} was not sent by the bot.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the first page cannot be generated.
	 */
	public static ActionReference lazyPaginate(@NotNull Message msg, List<Page> pageCache, @NotNull ThrowingFunction<Integer, Page> pageLoader, boolean useButtons) throws ErrorResponseException, InsufficientPermissionException {
		return lazyPaginate(msg, pageCache, pageLoader, useButtons, 0, null, null);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit.
	 * You can specify how long the listener will stay active before shutting itself down after a
	 * no-activity interval.
	 *
	 * @param msg        The {@link Message} sent which will be paginated.
	 * @param pageCache  The {@link List} that'll hold previously visited pages (can be pre-filled or edited anytime).
	 * @param pageLoader {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                   returns null, the method will treat it as the last page, preventing unnecessary updates.
	 * @param useButtons Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
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
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the first page cannot be generated.
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
	 *                    returns null, the method will treat it as the last page, preventing unnecessary updates.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param canInteract {@link Predicate} to determine whether the interaction is to be allowed.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the first page cannot be generated.
	 */
	public static ActionReference lazyPaginate(@NotNull Message msg, List<Page> pageCache, @NotNull ThrowingFunction<Integer, Page> pageLoader, boolean useButtons, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return lazyPaginate(msg, pageCache, pageLoader, useButtons, 0, null, canInteract);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit.
	 * You can specify how long the listener will stay active before shutting itself down after a
	 * no-activity interval.
	 *
	 * @param msg         The {@link Message} sent which will be paginated.
	 * @param pageCache   The {@link List} that'll hold previously visited pages (can be pre-filled or edited anytime).
	 * @param pageLoader  {@link ThrowingFunction}&lt;{@link Integer}, {@link Page}&gt; to generate the next page. If this
	 *                    returns null, the method will treat it as the last page, preventing unnecessary updates.
	 * @param useButtons  Whether to use interaction {@link Button} or reactions. Will fall back to false if the supplied
	 *                    {@link Message} was not sent by the bot.
	 * @param time        The time before the listener automatically stop listening
	 *                    for further events (recommended: 60).
	 * @param unit        The time's {@link TimeUnit} (recommended:
	 *                    {@link TimeUnit#SECONDS}).
	 * @param canInteract {@link Predicate} to determine whether the interaction is to be allowed.
	 * @return an {@link ActionReference} pointing to the newly created event, can be used for checking when it gets
	 * disposed of.
	 * @throws ErrorResponseException          Thrown if the {@link Message} no longer exists
	 *                                         or cannot be accessed when triggering a
	 *                                         {@link GenericMessageReactionEvent}.
	 * @throws InsufficientPermissionException Thrown if this library cannot remove reactions
	 *                                         due to lack of bot permission.
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the first page cannot be generated.
	 */
	public static ActionReference lazyPaginate(@NotNull Message msg, List<Page> pageCache, @NotNull ThrowingFunction<Integer, Page> pageLoader, boolean useButtons, int time, TimeUnit unit, Predicate<InteractionData> canInteract) throws ErrorResponseException, InsufficientPermissionException {
		return lazyPaginate(msg, new LazyPaginateHelper(pageLoader, pageCache, useButtons)
				.setTimeout(time, unit)
				.setCanInteract(canInteract)
		);
	}

	/**
	 * Adds navigation buttons to the specified {@link Message}/{@link MessageEmbed}
	 * which will lazily load content by using supplied {@link ThrowingFunction}. For this reason,
	 * this pagination type cannot have skip nor fast-forward buttons given the unknown page limit.
	 * This version uses a helper class to aid customization and allow usage of configurations.
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
	 * @throws InvalidStateException           Thrown if the library wasn't activated or the first page cannot be generated.
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

		String evt = getHandler().getEventId(msg);
		return getHandler().addEvent(evt, new EventData<>(helper, new ThrowingBiConsumer<>() {
			private int p = 0;
			private ScheduledFuture<?> timeout;
			private final Consumer<Void> success = s -> {
				if (timeout != null) {
					timeout.cancel(true);
				}
			};

			private final Function<Button, Button> LOWER_BOUNDARY_CHECK = b -> b.withDisabled(p == 0);

			{
				if (helper.getTimeout() > 0) {
					timeout = getScheduler().schedule(evt, () -> finalizeEvent(msg, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
				}
			}

			@Override
			public void acceptThrows(@NotNull User u, @NotNull PaginationEventWrapper wrapper) {
				Message msg = wrapper.retrieveMessage();
				if (msg == null || !wrapper.getMessageId().equals(msg.getId())) return;

				Action emt = NONE;
				if (wrapper.getContent() instanceof MessageReaction) {
					EmojiUnion reaction = ((MessageReaction) wrapper.getContent()).getEmoji();
					emt = toEmote(reaction);
				} else if (wrapper.getContent() instanceof Button) {
					Button btn = (Button) wrapper.getContent();

					if (btn.getCustomId() != null && Action.isNative(btn)) {
						emt = Action.valueOf(ButtonId.ID_PATTERN.split(btn.getCustomId())[0]);
					}
				}

				Page pg = null;
				boolean update = false;
				boolean end = false;
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
									end = true;
								}
							}
						} else {
							pg = helper.getPageLoader().apply(p);
							if (pg == null) {
								p--;
								end = true;
							}
						}

						if (cache && pg != null) {
							helper.getContent().add(pg);
						}
						break;
					case CANCEL:
						finalizeEvent(msg, wrapper.getHook(), success);
						return;
				}

				if (update) {
					boolean reachedEnd = end;
					modifyButtons(msg, pg, Map.of(
							PREVIOUS.name(), LOWER_BOUNDARY_CHECK,
							SKIP_BACKWARD.name(), LOWER_BOUNDARY_CHECK,
							GOTO_FIRST.name(), LOWER_BOUNDARY_CHECK,
							NEXT.name(), b -> b.withDisabled(reachedEnd)
					));
				}

				if (timeout != null) {
					timeout.cancel(true);
				}
				if (helper.getTimeout() > 0) {
					timeout = getScheduler().schedule(evt, () -> finalizeEvent(msg, success), helper.getTimeout(), TimeUnit.MILLISECONDS);
				}

				if (wrapper.isFromGuild() && wrapper.getSource() instanceof MessageReactionAddEvent && paginator.isRemoveOnReact()) {
					((MessageReaction) wrapper.getContent()).removeReaction(u).submit();
				}
			}
		}));
	}

	/**
	 * Utility method for re-fetching a message.
	 *
	 * @param msg The {@link Message} to be reloaded.
	 * @return The updated message instance.
	 */
	public static Message reloadMessage(@NotNull Message msg) {
		try {
			return subGet(msg.getChannel().retrieveMessageById(msg.getId()), msg);
		} catch (InsufficientPermissionException e) {
			return msg;
		}
	}

	/**
	 * Utility method for submitting a {@link RestAction} and awaiting its result for up to 5 seconds.
	 *
	 * @param future The {@link RestAction} to be executed.
	 * @param <T>    Return type for the {@link RestAction}.
	 * @return The {@link RestAction} result, or null should it fail.
	 */
	public static <T> T subGet(@NotNull RestAction<T> future) {
		try {
			return future.submit().get(5, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
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

	private static Action toEmote(EmojiUnion reaction) {
		return Action.getByEmoji(toEmoji(reaction));
	}

	private static Emoji toEmoji(EmojiUnion reaction) {
		return Emoji.fromFormatted(reaction.getFormatted());
	}

	/**
	 * Utility method to clear all reactions of a message.
	 *
	 * @param msg The {@link Message} to have reactions/buttons removed from.
	 */
	public static void clearReactions(Message msg) {
		User self = msg.getJDA().getSelfUser();
		if (!msg.getAuthor().equals(self) || msg.getReactions().isEmpty()) return;

		try {
			if (msg.getChannel().getType().isGuild()) {
				Guild guild = msg.getGuild();
				GuildMessageChannel channel = msg.getGuildChannel();
				if (guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE)) {
					subGet(msg.clearReactions());
					return;
				}
			}

			for (MessageReaction r : msg.getReactions()) {
				subGet(r.removeReaction());
			}
		} catch (InsufficientPermissionException | IllegalStateException e) {
			paginator.log(PUtilsConfig.LogLevel.LEVEL_2, "Not enough permissions to clear message reactions:", e);
		}
	}

	/**
	 * Utility method to clear all buttons of a message.
	 *
	 * @param msg The {@link Message} to have reactions/buttons removed from.
	 */
	public static void clearButtons(Message msg) {
		User self = msg.getJDA().getSelfUser();
		if (!msg.getAuthor().equals(self) || Pages.getButtons(msg).isEmpty()) return;

		subGet(msg.editMessageComponents());
	}

	/**
	 * Utility method to properly finalize a pagination event.
	 *
	 * @param msg      The source {@link Message} of the event.
	 * @param callback Action to be executed after finalizing.
	 */
	public static void finalizeEvent(Message msg, Consumer<Void> callback) {
		finalizeEvent(msg, null, callback);
	}

	/**
	 * Utility method to properly finalize a pagination event.
	 *
	 * @param msg      The source {@link Message} of the event.
	 * @param hook     The source {@link InteractionHook} of the event.
	 * @param callback Action to be executed after finalizing.
	 */
	public static void finalizeEvent(Message msg, InteractionHook hook, Consumer<Void> callback) {
		if (!msg.isEphemeral()) {
			msg = reloadMessage(msg);

			clearButtons(msg);
			clearReactions(msg);
		} else if (hook != null) {
			hook.deleteOriginal().queue();
		}

		callback.accept(null);

		getHandler().removeEvent(getHandler().getEventId(msg));
		if (paginator.isDeleteOnCancel()) msg.delete().submit();
	}

	/**
	 * Utility method for switching pages and/or modifying message buttons.
	 *
	 * @param msg     The {@link Message} holding the buttons.
	 * @param p       The current {@link Page}.
	 * @param changes {@link Map} containing desired changes, indexed by {@link Button} ID.
	 */
	public static void modifyButtons(Message msg, @Nullable Page p, Map<String, Function<Button, Button>> changes) {
		MessageEditAction act = msg.editMessageComponents();

		if (p != null) {
			if (p.getContent() instanceof String) {
				act = msg.editMessage((String) p.getContent());
			} else if (p.getContent() instanceof MessageEmbed) {
				act = msg.editMessageEmbeds((MessageEmbed) p.getContent());
			} else if (p.getContent() instanceof EmbedCluster) {
				act = msg.editMessageEmbeds(((EmbedCluster) p.getContent()).getEmbeds());
			}
		}

		act.setComponents(msg.getComponentTree().replace(c -> {
			if (c instanceof Button) {
				Button btn = (Button) c;
				String id = ButtonId.ID_PATTERN.split(btn.getCustomId())[0];

				if (changes.containsKey(id)) {
					return changes.get(id).apply(btn);
				}
			}

			return c;
		})).submit();
	}

	/**
	 * Utility method to add navigation buttons.
	 *
	 * @param msg      The {@link Message} to have reactions removed from.
	 * @param withSkip Whether to include {@link Action#SKIP_BACKWARD} and {@link Action#SKIP_FORWARD} buttons.
	 * @param withGoto Whether to include {@link Action#GOTO_FIRST} and {@link Action#GOTO_LAST} buttons.
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

	/**
	 * Utility method for extracting a {@link Message}'s {@link Button}s.
	 *
	 * @param msg The {@link Message} to extract buttons from.
	 * @return The {@link Message}'s {@link Button}s.
	 */
	public static List<Button> getButtons(Message msg) {
		return msg.getComponents().stream()
				.filter(c -> c instanceof ActionRow)
				.map(MessageTopLevelComponentUnion::asActionRow)
				.map(ActionRow::getButtons)
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}
}
