package com.github.ygimenez.method;

import com.github.ygimenez.exception.InvalidHandlerException;
import com.github.ygimenez.listener.EventHandler;
import com.github.ygimenez.model.*;
import com.github.ygimenez.model.helper.PaginateHelper;
import com.github.ygimenez.type.Action;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises one registered paginator as a Discord client would use it, without network access. */
class PaginateActionsTest {

	private JDA jda;
	private Message message;
	private User user;
	private InteractionHook hook;
	private MessageEditAction edit;
	private TaskScheduler scheduler;
	private ScheduledFuture<?> scheduledTask;
	private EventHandler handler;
	private List<MessageTopLevelComponent> components;
	private List<MessageTopLevelComponent> pendingComponents;
	private CompletableFuture<Message> nextEditResult;
	private RestAction<Void> reactionAction;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() throws InvalidHandlerException {
		jda = mock(JDA.class);
		SelfUser self = mock(SelfUser.class);
		when(jda.getSelfUser()).thenReturn(self);
		when(self.getId()).thenReturn("10");
		user = mock(User.class);
		when(user.getId()).thenReturn("11");
		hook = mock(InteractionHook.class, RETURNS_DEEP_STUBS);
		when(hook.getInteraction().getUser()).thenReturn(user);
		message = mock(Message.class, RETURNS_DEEP_STUBS);
		when(message.getId()).thenReturn("30");
		when(message.getJDA()).thenReturn(jda);
		when(message.getAuthor()).thenReturn(self);
		when(message.getChannel().getId()).thenReturn("20");
		when(message.getChannel().getType()).thenReturn(ChannelType.PRIVATE);
		when(message.getReactions()).thenReturn(Collections.emptyList());
		components = new ArrayList<>();
		pendingComponents = null;
		nextEditResult = null;
		when(message.getComponents()).thenAnswer(invocation -> components);

		RestAction<Message> retrieve = mock(RestAction.class);
		when(retrieve.submit()).thenReturn(CompletableFuture.completedFuture(message));
		when(message.getChannel().retrieveMessageById(message.getId())).thenReturn(retrieve);
		edit = mock(MessageEditAction.class, RETURNS_SELF);
		when(edit.getJDA()).thenReturn(jda);
		when(edit.submit()).thenAnswer(invocation -> {
			List<MessageTopLevelComponent> submittedComponents = pendingComponents;
			pendingComponents = null;
			CompletableFuture<Message> result = nextEditResult == null
					? CompletableFuture.completedFuture(message) : nextEditResult;
			nextEditResult = null;
			return result.thenApply(updated -> {
				if (submittedComponents != null) components = submittedComponents;
				return updated;
			});
		});
		when(edit.setComponents(anyCollection())).thenAnswer(invocation -> {
			pendingComponents = new ArrayList<>(invocation.<Collection<MessageTopLevelComponent>>getArgument(0));
			return edit;
		});
		when(message.editMessageComponents()).thenReturn(edit);
		when(message.editMessage(anyString())).thenReturn(edit);
		reactionAction = mock(RestAction.class);
		when(reactionAction.getJDA()).thenReturn(jda);
		when(reactionAction.submit()).thenReturn(CompletableFuture.completedFuture(null));
		when(reactionAction.submit(anyBoolean())).thenReturn(CompletableFuture.completedFuture(null));
		when(message.addReaction(any(Emoji.class))).thenReturn(reactionAction);
		when(message.removeReaction(any(Emoji.class))).thenReturn(reactionAction);

		handler = new EventHandler();
		scheduler = mock(TaskScheduler.class);
		scheduledTask = mock(ScheduledFuture.class);
		doReturn(scheduledTask).when(scheduler).schedule(anyString(), any(Runnable.class), anyLong(), any(TimeUnit.class));
		Paginator paginator = mock(Paginator.class);
		when(paginator.getHandler()).thenReturn(jda);
		when(paginator.getEvtHandler()).thenReturn(handler);
		when(paginator.getScheduler()).thenReturn(scheduler);
		when(paginator.getEmotes()).thenReturn(Collections.emptyMap());
		when(paginator.getEmoji(any(Action.class))).thenAnswer(invocation -> invocation.<Action>getArgument(0).getEmoji());
		Pages.activate(paginator);
	}

	@AfterEach
	void tearDown() {
		Pages.deactivate();
	}

	@Test
	void navigationAndCustomActionsWorkInOneRegistration() {
		List<Integer> selectedPages = new ArrayList<>();
		AtomicReference<ButtonWrapper> selected = new AtomicReference<>();
		PaginateHelper helper = buttons(3).addAction(new TextId("choose", "Choose"), (button, page) -> {
			selectedPages.add(page);
			selected.set(button);
		});
		Pages.paginate(message, helper);
		assertEquals(1, handler.getEventMap().size());
		assertTrue(navigation(Action.PREVIOUS).isDisabled());
		press(navigation(Action.NEXT));
		Button choose = labelled("Choose");
		press(choose);
		assertEquals(List.of(1), selectedPages);
		assertSame(user, selected.get().getUser());
		assertSame(hook, selected.get().getHook());
		assertSame(message, selected.get().getMessage());
		assertSame(choose, selected.get().getButton());
		verify(message).editMessage("Page 1");
		press(navigation(Action.PREVIOUS));
		press(labelled("Choose"));
		assertEquals(List.of(1, 0), selectedPages);
		assertEquals(1, handler.getEventMap().size());
	}

	@Test
	void pageSpecificOptionsChangeFromFiveToFiveToTwoAndRejectStaleButtons() {
		List<String> selected = new ArrayList<>();
		PaginateHelper helper = buttons(3);
		for (int page = 0; page < 3; page++) {
			int expectedPage = page;
			for (int option = 1; option <= (page == 2 ? 2 : 5); option++) {
				int expectedOption = option;
				helper.addPageAction(page, new TextId("option" + option, "Option " + option), (button, index) ->
						selected.add(expectedPage + ":" + expectedOption + ":" + index));
			}
		}
		Pages.paginate(message, helper);
		assertEquals(5, optionCount());
		Button stale = labelled("Option 1");
		press(navigation(Action.NEXT));
		assertEquals(5, optionCount());
		press(stale);
		assertTrue(selected.isEmpty(), "A click from the previous page must not choose the new page's option");
		press(labelled("Option 1"));
		assertEquals(List.of("1:1:1"), selected);
		press(navigation(Action.NEXT));
		assertEquals(2, optionCount());
		assertTrue(navigation(Action.NEXT).isDisabled());
		press(labelled("Option 2"));
		assertEquals(List.of("1:1:1", "2:2:2"), selected);
	}

	@Test
	void pageActionOverridesGlobalActionWithoutRemovingOtherGlobalControls() {
		List<String> selected = new ArrayList<>();
		PaginateHelper helper = buttons(2)
				.addAction(new TextId("choose", "Choose"), button -> selected.add("global"))
				.addAction(new TextId("help", "Help"), button -> selected.add("help"))
				.addPageAction(1, new TextId("choose", "Choose"), button -> selected.add("page"));
		Pages.paginate(message, helper);
		press(labelled("Choose"));
		press(navigation(Action.NEXT));
		press(labelled("Choose"));
		press(labelled("Help"));
		assertEquals(List.of("global", "page", "help"), selected);
	}

	@Test
	void reactionsNavigateAndDispatchEmojiActionsWithExplicitIds() {
		List<Integer> selectedPages = new ArrayList<>();
		AtomicReference<ButtonWrapper> selected = new AtomicReference<>();
		Emoji emoji = Emoji.fromUnicode("⭐");
		PaginateHelper helper = reactions(2).addAction(new EmojiId("favorite", emoji, "Favorite", ButtonStyle.SUCCESS), (button, page) -> {
			selectedPages.add(page);
			selected.set(button);
		});
		Pages.paginate(message, helper);
		verify(message).addReaction(emoji);
		react(Action.NEXT.getEmoji());
		react(emoji);
		react(Action.PREVIOUS.getEmoji());
		react(emoji);
		assertEquals(List.of(1, 0), selectedPages);
		assertSame(user, selected.get().getUser());
		assertNull(selected.get().getHook());
		assertNull(selected.get().getButton());
		assertEquals(1, handler.getEventMap().size());
	}

	@Test
	void reactionsForAnInactivePageDoNotInvokeItsActions() {
		AtomicInteger selected = new AtomicInteger();
		Emoji onlyFirstPage = Emoji.fromUnicode("⭐");
		Emoji onlySecondPage = Emoji.fromUnicode("❤");
		Pages.paginate(message, reactions(2)
				.addPageAction(0, onlyFirstPage, button -> selected.addAndGet(1))
				.addPageAction(1, onlySecondPage, button -> selected.addAndGet(10)));
		react(onlyFirstPage);
		react(Action.NEXT.getEmoji());
		react(onlyFirstPage);
		react(onlySecondPage);
		assertEquals(11, selected.get());
		verify(message).addReaction(onlySecondPage);
	}

	@Test
	void acceptEmojiRemainsAvailableAsACustomReaction() {
		AtomicInteger selected = new AtomicInteger();
		Pages.paginate(message, reactions(2).addAction(Action.ACCEPT.getEmoji(), button -> selected.incrementAndGet()));
		react(Action.ACCEPT.getEmoji());
		assertEquals(1, selected.get());
		assertEquals(1, handler.getEventMap().size());
	}

	@Test
	void acceptEmojiRemainsAvailableAsACustomButton() {
		AtomicInteger selected = new AtomicInteger();
		Pages.paginate(message, buttons(2).addAction(Action.ACCEPT.getEmoji(), button -> selected.incrementAndGet()));
		Button accept = currentButtons().stream().filter(button -> Action.ACCEPT.getEmoji().equals(button.getEmoji()))
				.findFirst().orElseThrow(AssertionError::new);
		press(accept);
		assertEquals(1, selected.get());
	}

	@Test
	void registeredActionsAreIndependentOfLaterHelperChanges() {
		AtomicInteger selected = new AtomicInteger();
		PaginateHelper helper = buttons(2).addAction(new TextId("choose", "Choose"), button -> selected.incrementAndGet());
		Pages.paginate(message, helper);
		helper.clearActions().clearPages();
		press(navigation(Action.NEXT));
		press(labelled("Choose"));
		assertEquals(1, selected.get());
		verify(message).editMessage("Page 1");
	}

	@Test
	void failedPageEditKeepsTheCurrentPageAndAllowsARetry() {
		List<Integer> selectedPages = new ArrayList<>();
		Pages.paginate(message, buttons(2).addAction(new TextId("choose", "Choose"), (button, page) -> selectedPages.add(page)));
		Button oldNext = navigation(Action.NEXT);
		Button oldChoose = labelled("Choose");
		CompletableFuture<Message> pending = new CompletableFuture<>();
		nextEditResult = pending;
		press(oldNext);
		press(oldChoose);
		assertTrue(selectedPages.isEmpty(), "Clicks must wait until the pending page edit completes");
		pending.completeExceptionally(new IllegalStateException("Discord rejected the page edit"));
		press(oldChoose);
		assertEquals(List.of(0), selectedPages);
		press(oldNext);
		press(labelled("Choose"));
		assertEquals(List.of(0, 1), selectedPages);
		verify(message, times(2)).editMessage("Page 1");
	}

	@Test
	void anOldTimeoutCannotExpireAPendingPageEdit() {
		List<Integer> selectedPages = new ArrayList<>();
		Pages.paginate(message, buttons(2).setTimeout(15, TimeUnit.SECONDS)
				.addAction(new TextId("choose", "Choose"), (button, page) -> selectedPages.add(page)));
		ArgumentCaptor<Runnable> timer = ArgumentCaptor.forClass(Runnable.class);
		verify(scheduler).schedule(anyString(), timer.capture(), eq(15000L), eq(TimeUnit.MILLISECONDS));
		CompletableFuture<Message> pending = new CompletableFuture<>();
		nextEditResult = pending;
		press(navigation(Action.NEXT));
		timer.getValue().run();
		assertEquals(1, handler.getEventMap().size());
		pending.complete(message);
		press(labelled("Choose"));
		assertEquals(List.of(1), selectedPages);
	}

	@Test
	void failedInitialEditRemovesTheUnusableRegistration() {
		nextEditResult = CompletableFuture.failedFuture(new IllegalStateException("Discord rejected initial controls"));
		Pages.paginate(message, buttons(2).addAction(new TextId("choose", "Choose"), button -> {}));
		assertTrue(handler.getEventMap().isEmpty());
		assertTrue(components.isEmpty());
	}

	@Test
	void failedOldSetupDoesNotRemoveANewerRegistrationForTheMessage() {
		CompletableFuture<Message> oldSetup = new CompletableFuture<>();
		nextEditResult = oldSetup;
		Pages.paginate(message, buttons(2).addAction(new TextId("old", "Old"), button -> fail("Old callback must not run")));
		AtomicInteger selected = new AtomicInteger();
		Pages.paginate(message, buttons(2).addAction(new TextId("new", "New"), button -> selected.incrementAndGet()));
		oldSetup.completeExceptionally(new IllegalStateException("The replaced setup failed"));
		assertEquals(1, handler.getEventMap().size());
		press(labelled("New"));
		assertEquals(1, selected.get());
	}

	@Test
	void oneTimeoutIsRenewedForRecognizedActionsOnly() {
		Pages.paginate(message, buttons(2).setTimeout(15, TimeUnit.SECONDS)
				.addAction(new TextId("choose", "Choose"), button -> {}));
		verify(scheduler).schedule(eq(handler.getEventId(message)), any(Runnable.class), eq(15000L), eq(TimeUnit.MILLISECONDS));
		press(Button.secondary("unmapped", "Unknown"));
		verifyNoMoreInteractions(scheduler);
		press(labelled("Choose"));
		ArgumentCaptor<Runnable> timers = ArgumentCaptor.forClass(Runnable.class);
		verify(scheduler, times(2)).schedule(eq(handler.getEventId(message)), timers.capture(), eq(15000L), eq(TimeUnit.MILLISECONDS));
		verify(scheduledTask).cancel(anyBoolean());
		timers.getAllValues().get(0).run();
		assertEquals(1, handler.getEventMap().size(), "An already queued old timer must not expire the renewed session");
		timers.getAllValues().get(1).run();
		assertTrue(handler.getEventMap().isEmpty());
	}

	@Test
	void cancelRemovesTheCombinedRegistrationAndCancelsItsTimeout() {
		Pages.paginate(message, buttons(2).setTimeout(15, TimeUnit.SECONDS)
				.addAction(new TextId("choose", "Choose"), button -> {}));
		press(navigation(Action.CANCEL));
		assertTrue(handler.getEventMap().isEmpty());
		verify(scheduledTask).cancel(anyBoolean());
	}

	@Test
	void disablingCancellationOmitsAndIgnoresCancelReactions() {
		AtomicInteger selected = new AtomicInteger();
		Pages.paginate(message, reactions(2).setCancellable(false)
				.addAction(Emoji.fromUnicode("⭐"), button -> selected.incrementAndGet()));
		verify(message, never()).addReaction(Action.CANCEL.getEmoji());
		react(Action.CANCEL.getEmoji());
		assertEquals(1, handler.getEventMap().size());
		react(Emoji.fromUnicode("⭐"));
		assertEquals(1, selected.get());
	}

	@Test
	void canInteractAppliesToCustomButtonsAsWellAsNavigation() {
		AtomicInteger selected = new AtomicInteger();
		Pages.paginate(message, buttons(2).setCanInteract(data -> false)
				.addAction(new TextId("choose", "Choose"), button -> selected.incrementAndGet()));
		ButtonInteractionEvent event = buttonEvent(labelled("Choose"));
		when(event.deferEdit().submit()).thenReturn(CompletableFuture.completedFuture(hook));
		handler.onButtonInteraction(event);
		assertEquals(0, selected.get());
		event = buttonEvent(navigation(Action.NEXT));
		when(event.deferEdit().submit()).thenReturn(CompletableFuture.completedFuture(hook));
		handler.onButtonInteraction(event);
		verify(message, never()).editMessage(anyString());
	}

	@Test
	void invalidNativeIdIsRejectedBeforeEditingOrRegistering() {
		PaginateHelper helper = buttons(2).addAction(new TextId("NEXT", "Choose"), button -> {});
		assertThrows(IllegalArgumentException.class, () -> Pages.paginate(message, helper));
		assertTrue(handler.getEventMap().isEmpty());
		verify(message, never()).editMessageComponents();
	}

	@Test
	void reactionNavigationEmojiCollisionIsRejectedBeforeMutation() {
		PaginateHelper helper = reactions(2).addAction(Action.NEXT.getEmoji(), button -> {});
		assertThrows(IllegalArgumentException.class, () -> Pages.paginate(message, helper));
		verify(message, never()).addReaction(any(Emoji.class));
	}

	@Test
	void textActionsAreRejectedWhenMessageOwnershipForcesReactionFallback() {
		when(message.getAuthor()).thenReturn(user);
		PaginateHelper helper = buttons(2).addAction(new TextId("choose", "Choose"), button -> {});
		assertThrows(IllegalArgumentException.class, () -> Pages.paginate(message, helper));
		assertTrue(handler.getEventMap().isEmpty());
		verify(message, never()).addReaction(any(Emoji.class));
	}

	@Test
	void twentyCustomButtonsFitAfterOneNavigationRowButTwentyOneDoNot() {
		PaginateHelper helper = buttons(2);
		for (int i = 0; i < 20; i++) helper.addAction(new TextId("choice" + i), button -> {});
		Pages.paginate(message, helper);
		assertEquals(5, components.size());
		assertTrue(components.stream().allMatch(row -> ((ActionRow) row).getButtons().size() <= 5));
		helper.addAction(new TextId("overflow"), button -> {});
		assertThrows(IllegalArgumentException.class, () -> helper.validateActions(true));
	}

	@Test
	void skipAndFastForwardReserveTwoRowsBeforeCustomButtons() {
		PaginateHelper helper = buttons(3).setSkipAmount(2).setFastForward(true);
		for (int i = 0; i < 15; i++) helper.addAction(new TextId("choice" + i), button -> {});
		assertEquals(5, helper.getComponents(0).size());
		helper.addAction(new TextId("overflow"), button -> {});
		assertThrows(IllegalArgumentException.class, () -> helper.validateActions(true));
	}

	@Test
	void reactionLimitIncludesNavigationAndCustomControls() {
		PaginateHelper helper = reactions(2);
		for (int i = 0; i < 17; i++) {
			helper.addAction(Emoji.fromCustom("option", 1000L + i, false), button -> {});
		}
		assertDoesNotThrow(() -> helper.validateActions(false));
		helper.addAction(Emoji.fromCustom("overflow", 2000L, false), button -> {});
		assertThrows(IllegalArgumentException.class, () -> helper.validateActions(false));
	}

	@Test
	void cloningPreservesSettingsAndIsolatesActionCollections() {
		PaginateHelper original = buttons(3).setSkipAmount(2).setFastForward(true)
				.setCancellable(false).setTimeout(7, TimeUnit.SECONDS).setCanInteract(data -> false)
				.addAction(new TextId("global"), button -> {})
				.addPageAction(1, new TextId("local"), button -> {});
		PaginateHelper copy = original.clone();
		assertEquals(2, copy.getSkipAmount());
		assertTrue(copy.isFastForward());
		assertFalse(copy.isCancellable());
		assertEquals(7000, copy.getTimeout());
		assertFalse(copy.canInteract(new InteractionData("test", message, user)));
		copy.clearActions();
		copy.clearPages();
		assertEquals(1, original.getActions().size());
		assertEquals(2, original.getActions(1).size());
		assertEquals(3, original.getContent().size());
	}

	private PaginateHelper buttons(int count) {
		List<Page> pages = new ArrayList<>();
		for (int i = 0; i < count; i++) pages.add(InteractPage.of("Page " + i));
		return new PaginateHelper(pages, true);
	}

	private PaginateHelper reactions(int count) {
		List<Page> pages = new ArrayList<>();
		for (int i = 0; i < count; i++) pages.add(Page.of("Page " + i));
		return new PaginateHelper(pages, false);
	}

	private List<Button> currentButtons() {
		return components.stream().flatMap(row -> ((ActionRow) row).getButtons().stream()).collect(Collectors.toList());
	}

	private Button navigation(Action action) {
		return currentButtons().stream().filter(button -> Action.fromButton(button) == action).findFirst()
				.orElseThrow(() -> new AssertionError("Missing navigation button: " + action));
	}

	private Button labelled(String label) {
		return currentButtons().stream().filter(button -> label.equals(button.getLabel())).findFirst()
				.orElseThrow(() -> new AssertionError("Missing custom button: " + label));
	}

	private long optionCount() {
		return currentButtons().stream().filter(button -> button.getLabel().startsWith("Option ")).count();
	}

	private ButtonInteractionEvent buttonEvent(Button button) {
		ButtonInteractionEvent event = mock(ButtonInteractionEvent.class, RETURNS_DEEP_STUBS);
		when(event.getHook()).thenReturn(hook);
		when(event.getButton()).thenReturn(button);
		when(event.getComponentId()).thenReturn(button.getCustomId());
		when(event.getMessage()).thenReturn(message);
		doReturn(message.getId()).when(event).getMessageId();
		doReturn(message.getChannel()).when(event).getChannel();
		when(event.getUser()).thenReturn(user);
		return event;
	}

	private void press(Button button) {
		ButtonInteractionEvent event = buttonEvent(button);
		when(event.deferEdit().submit()).thenReturn(CompletableFuture.completedFuture(hook));
		handler.onButtonInteraction(event);
		verify(event.deferEdit()).submit();
	}

	private void react(Emoji emoji) {
		MessageReaction reaction = mock(MessageReaction.class);
		when(reaction.getEmoji()).thenReturn(Emoji.fromFormatted(emoji.getFormatted()));
		MessageReactionAddEvent event = mock(MessageReactionAddEvent.class, RETURNS_DEEP_STUBS);
		when(event.getReaction()).thenReturn(reaction);
		doReturn(message.getChannel()).when(event).getChannel();
		doReturn(message.getId()).when(event).getMessageId();
		when(event.getUser()).thenReturn(user);
		when(event.retrieveMessage().submit()).thenReturn(CompletableFuture.completedFuture(message));
		when(event.retrieveUser().submit()).thenReturn(CompletableFuture.completedFuture(user));
		handler.onMessageReactionAdd(event);
	}
}
