[mvncentral]: https://mvnrepository.com/artifact/com.github.ygimenez/Pagination-Utils

[jitpack]: https://jitpack.io/#ygimenez/Pagination-Utils

[build]: https://github.com/ygimenez/Pagination-Utils/tree/master

[license]: https://github.com/ygimenez/Pagination-Utils/blob/master/LICENSE

[issue]: https://github.com/ygimenez/Pagination-Utils/issues

[mvncentral-shield]: https://img.shields.io/maven-central/v/com.github.ygimenez/Pagination-Utils?label=Maven%20Central

[jitpack-shield]: https://img.shields.io/badge/Download-Jitpack-success

[build-shield]: https://img.shields.io/github/actions/workflow/status/ygimenez/Pagination-Utils/maven.yml?label=Build

[license-shield]: https://img.shields.io/github/license/ygimenez/Pagination-Utils?color=lightgrey&label=License

[issue-shield]: https://img.shields.io/github/issues/ygimenez/Pagination-Utils?label=Issues

[ ![mvncentral-shield][] ][mvncentral]
[ ![jitpack-shield][] ][jitpack]
[ ![build-shield][] ][build]
[ ![license-shield][] ][license]
[ ![issue-shield][] ][issue]

<img align="right" src="https://raw.githubusercontent.com/ygimenez/Pagination-Utils/master/icon.png" height=225 width=225 alt="Pagination Utils logo">

# Pagination Utils - JDA Pagination made easier!

With this library you will be using pages, categories and buttons in your bot in no time!

This branch targets **JDA 6.7.0**.

## What is a page/category/button?

![Pagination Example](https://i.imgur.com/k5zA0ix.gif)

![Categorization Example](https://i.imgur.com/9xhs4Gf.gif)

Have you been using a bot and came across one of those GIFs and thought: "That must've been hard to make." or "I'd
like one of those in my bot!"? Fear no more, Pagination Utils to the rescue!

## How do I paginate?

Before we start the fun stuff, there are a few things we need to check:

- You're using Java JDK 9 or above.
- Your bot has the following permissions:
  - MESSAGE_READ/WRITE
  - VIEW_CHANNEL
  - If using reactions:
    - MESSAGE_ADD_REACTION
    - MESSAGE_EXT_EMOJI
- If using `JDABuilder.createLight()`, you added the following gateway intents:
  - GUILD_MESSAGES
  - GUILD_MESSAGE_REACTIONS

Now we can finally start, which is easier than it seems! The first step is to set a JDA/SharmManager client object as the event
holder, which can be done in a single line:

```java
JDA bot = ... // Creation of bot client

Pages.activate(PaginatorBuilder.createSimplePaginator(bot));
```

But if you want some customization of the library's behavior, you can opt to use the full builder:

```java
JDA bot = ... // Creation of bot client

Paginator paginator = PaginatorBuilder.createPaginator(bot)
// Whether reactions will be removed on click
	.shouldRemoveOnReaction(false)
        // Prevents double-click on buttons and guarantee an event will only happen when previous processing has finished
        .shouldEventLock(true)
        // Whether to delete the message when the event ends (such as pressing CANCEL or timeout)
        .shouldDeleteOnCancel(true)
        // Finish configuration and activate the library
        .activate();
```

If you want to go even further and change the default buttons' actions, don't worry; we got you covered:

```java
JDA bot = ... // Creation of bot client

Paginator paginator = PaginatorBuilder.createPaginator(bot)
        // Whether reactions will be removed on click
        .shouldRemoveOnReaction(false)
        // Prevents double-click on buttons and guarantee an event will only happen when previous processing has finished
        .shouldEventLock(true)
        // Whether to delete the message when the event ends (such as pressing CANCEL or timeout)
        .shouldDeleteOnCancel(true)
        // Changes the next button to 😙
        .setEmote(Emote.NEXT, Emoji.fromFormatted("😙"))
        // Changes the previous button to 😩
        .setEmote(Emote.PREVIOUS, Emoji.fromFormatted("😩"))
        // Finish configuration and activate the library
        .activate();
```

Then all you need to do is create a `Page` (or `InteractPage` for interaction buttons) collection containing the type of the content and the `Message`/`MessageEmbed` object that you just created.

Example:

```java
// Example using MessageBuilder
MessageBuilder mb = new MessageBuilder();
mb.setContent("Hello World!");

Page messagePage = Page.of(mb.build());

// Example using EmbedBuilder
EmbedBuilder eb = new EmbedBuilder()
        .setTitle("Example Embed")
        .setDescription("Hello World");

Page embedPage = InteractPage.of(eb.build());
```

That said, you might want to create a `List` of pages to use the pagination, since a single page does not need to be paginated at all:

```java
List<Page> pages = new ArrayList<>();

// Adding 10 pages to the list
for (int i = 0; i < 10; i++) {
        pages.add(InteractPage.of("This is entry Nº " + (i + 1)));
}
```

Then all you have to do is call `Pages.paginate()` method:

```java
exampleChannel.sendMessage((Message) pages.get(0).getContent()).queue(success -> {
	Pages.paginate(success, pages, /* Use buttons? */ true);
});
```

<figure class="image">
  <img src="https://i.imgur.com/Ms6ECNY.png" alt="Result">
  <figcaption><i style="color: gray">Wait, that's it?</i></figcaption>
</figure>

That's everything you have to do; the library will automatically add the navigation buttons to the target message, which
will change its content based on the list's order.

## How do I categorize?

To categorize it's almost the same process as paginating, however, the type of collection is `HashMap` instead
of `ArrayList`:

```java
Map<Emoji, Page> categories = new HashMap<>();

// Manually adding 3 categories to the map, you could use some kind of loop to fill it (see https://emojipedia.org/ for unicodes)
mb.setContent("This is category 1");
categories.put(Emoji.fromFormatted("\u26f3"), InteractPage.of("This is category 1"));

mb.setContent("This is category 2");
categories.put(Emoji.fromFormatted("\u26bd"), InteractPage.of("This is category 2"));

mb.setContent("This is category 3");
categories.put(Emoji.fromFormatted("\u270f"), InteractPage.of("This is category 3"));
```

Then just call `Pages.categorize()` method just like you did before:

```java
exampleChannel.sendMessage("This is the main menu").queue(success -> {
	Pages.categorize(success, categories, /* Use buttons? */ true);
});
```

<figure class="image">
  <img src="https://i.imgur.com/MeqRUPp.png" alt="Result">
  <figcaption><i style="color: gray">Of course you could be more elaborate.</i></figcaption>
</figure>

## How do I buttonize?

To do it, you first need to set up a few things:

```java
// A Consumer is a callback function that uses one arguments and returns nothing
// Here, wrapper is a class containing useful data related to the click event
ThrowingConsumer<ButtonWrapper> customFunction = (wrapper) -> {
	// Example of giving a role to anyone who presses this button
	guild.addRoleToMember(wrapper.getMember(), guild.getRoleById("123456789")).queue();
};

exampleChannel.sendMessage("Click to get role").queue(success -> {
	Pages.buttonize(success, Collections.singletonMap(Emoji.fromFormatted("✅"), customFunction), /* Use buttons? */ true, /* Show cancel? */ false);
});
```

<figure class="image">
  <img src="https://i.imgur.com/fDF6MdO.png" alt="Result">
  <figcaption><i style="color: gray">Useful for self-assigned roles.</i></figcaption>
</figure>

## How do I combine pagination and custom actions?

Add the actions to a `PaginateHelper` and register it with **one** `Pages.paginate(message, helper)` call.
Navigation and custom actions then share the same interaction filter, inactivity timeout, and cancel button.
This addresses [issue #13](https://github.com/ygimenez/Pagination-Utils/issues/13).

`addAction` adds an action available on every page. Its two-argument callback receives a `ButtonWrapper` and the
**zero-based current page index**:

```java
PaginateHelper helper = new PaginateHelper(pages, true)
        .addAction(new TextId("select", "Select this page"), (button, pageIndex) -> {
            button.getChannel().sendMessage("Selected page " + (pageIndex + 1)).queue();
        })
        .setTimeout(60, TimeUnit.SECONDS);

exampleChannel.sendMessage((String) pages.get(0).getContent())
        .queue(message -> Pages.paginate(message, helper));
```

The example above assumes string-backed `InteractPage` entries. For embed pages, send the first page with
`sendMessageEmbeds` instead. The one-argument callback overload is available when the action does not need the page index.

### Different options on each page

Use `addPageAction` for actions that should only appear on a particular page. Here is the twelve-option example
from the issue, with five, five, and two options respectively:

```java
List<Page> pages = List.of(
        InteractPage.of("Options 1–5"),
        InteractPage.of("Options 6–10"),
        InteractPage.of("Options 11–12")
);

PaginateHelper helper = new PaginateHelper(pages, true)
        .setTimeout(60, TimeUnit.SECONDS);

for (int option = 0; option < 12; option++) {
    final int selectedOption = option + 1;
    int pageIndex = option / 5;
    int slot = option % 5 + 1;

    helper.addPageAction(pageIndex, new TextId("option-" + slot, "Option " + slot),
            (button, currentPage) -> button.getChannel()
                    .sendMessage("Selected option " + selectedOption).queue());
}

exampleChannel.sendMessage((String) pages.get(0).getContent())
        .queue(message -> Pages.paginate(message, helper));
```

Relevant imports are `com.github.ygimenez.model.helper.PaginateHelper`, `com.github.ygimenez.model.TextId`,
`com.github.ygimenez.model.Page`, and `com.github.ygimenez.model.InteractPage`.

A page-specific action overrides a global action with the same ID on that page. Buttons are rebuilt when navigating,
and outdated custom buttons are ignored. Reusing a helper for another message gives that message independent page state.

For reaction controls, pass `false` to the helper constructor and use `Emoji` or `EmojiId` actions. Text-only actions
cannot be represented as reactions and are rejected, including when pagination falls back to reactions on another user's
message. Removing other users' obsolete reactions requires `MESSAGE_MANAGE`; without it the bot removes its own reactions,
and actions not available on the current page are ignored.

Navigation IDs and navigation reaction emojis are reserved. The combined controls must fit within five action rows of
five buttons, or twenty distinct reactions. Invalid combinations are rejected before modifying the message.

Calling `Pages.paginate` and `Pages.buttonize` separately still replaces the message's registered handler. Migrate those
two calls to the combined helper above.

## I have low memory, what about me?

Yet again, don't worry, Pagination Utils to the rescue!

```java
// Could be anything, this is just an example.
List<String> data = new ArrayList<>();
// Adding 10 values pages to the list
for (int i = 0; i < 2; i++) {
	data.add("This is page number " + (i + 1));
}

ThrowingFunction<Integer, Page> func = i -> {
	return InteractPage.of(data.get(i));
};
```

Then just call `Pages.lazyPaginate()` method:

```java
// You can disable page caching, which will prevent the library from saving previously visited pages
exampleChannel.sendMessage((String) pages.get(0).getContent()).queue(success -> {
	Pages.lazyPaginate(success, func, /* Use buttons? */ true, /* Cache? */ false);
});
```

<figure class="image">
  <img src="https://i.imgur.com/Ms6ECNY.png" alt="Result">
  <figcaption><i style="color: gray">Same image because...well...both are paginators</i></figcaption>
</figure>

## Is it really that easy?

Yes, you can focus on creating epic menus, ranking, lists, games, *et cetera* and leave the boring part for the library to do its job; isn't that awesome?

## How do I get it?

This library is available for manual installation and through Maven Central:

### To install manually

* Click on the releases tab on the top of this repository
* Download the latest release
* Put the .jar file somewhere in your project
* Add it to the buildpath
* Done!

### To install via Maven Central

* Add this library as a dependency:

Gradle:

```gradle
dependencies {
    implementation group: 'com.github.ygimenez', name: 'Pagination-Utils', version: 'VERSION'
}
```

Maven:

```xml

<dependency>
  <groupId>com.github.ygimenez</groupId>
  <artifactId>Pagination-Utils</artifactId>
  <version>VERSION</version>
</dependency>
```

* Replace `VERSION` with the one shown here (without "v"): [ ![mvncentral-shield][] ][mvncentral]
* Done!

## Building and testing

Use JDK 17 or newer for the test toolchain. The library's compilation target remains Java 9.

```sh
mvn -B test
mvn -B -Dgpg.skip=true verify
```

The tests simulate Discord messages, component interactions, reactions, and timeout callbacks locally; they do not require
a bot token. The `verify` command also builds the main, source, and Javadoc JARs. GPG signing is skipped for local verification.

## Feedback

If you have any issues using this library, feel free to create a new issue; I'll review it as soon as possible!
