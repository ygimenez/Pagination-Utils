[mvncentral]: https://mvnrepository.com/artifact/com.github.ygimenez/Pagination-Utils

[jitpack]: https://jitpack.io/#ygimenez/Pagination-Utils

[build]: https://github.com/ygimenez/Pagination-Utils/tree/master

[license]: https://github.com/ygimenez/Pagination-Utils/blob/master/LICENSE

[issue]: https://github.com/ygimenez/Pagination-Utils/issues

[mvncentral-shield]: https://img.shields.io/maven-central/v/com.github.ygimenez/Pagination-Utils?label=Maven%20Central

[jitpack-shield]: https://img.shields.io/badge/Download-Jitpack-success

[build-shield]: https://img.shields.io/github/workflow/status/ygimenez/Pagination-Utils/Java%20CI?label=Build

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

But if you want some customization of the library's behaviour, you can opt to use the full builder:

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

If you want to go even further and change the default buttons' emotes, don't worry, we got you covered:

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

Page messagePage = new Page(mb.build());

// Example using EmbedBuilder
EmbedBuilder eb = new EmbedBuilder()
        .setTitle("Example Embed")
        .setDescription("Hello World");

Page embedPage = new InteractPage(eb.build());
```

That said, you might want to create a `List` of pages to use the pagination, since a single page does not need to be paginated at all:

```java
List<Page> pages = new ArrayList<>();

// Adding 10 pages to the list
for (int i = 0; i < 10; i++) {
	pages.add(new InteractPage("This is entry Nº " + (i + 1)));
}
```

Then all you have to do is call `Pages.paginate()` method:

```java
// This method requires 3 arguments:
// The target message
// The list of pages
// Whether you want to use reactions (false) or interaction buttons (true).
exampleChannel.sendMessage((Message) pages.get(0).getContent()).queue(success -> {
	Pages.paginate(success, pages, /* Use buttons? */ true);
});
```



That's everything you have to do, the library will automatically add the navigation buttons to the target message, which
will change its content based on the list's order.

## How do I categorize?

To categorize it's almost the same process as paginating, however, the type of collection is `HashMap` instead
of `ArrayList`:

```java
Map<Emoji, Page> categories = new HashMap<>();
MessageBuilder mb = new MessageBuilder();

// Manually adding 3 categories to the map, you could use some kind of loop to fill it (see https://emojipedia.org/ for unicodes)
mb.setContent("This is category 1");
categories.put(Emoji.fromFormatted("\u26f3"), new InteractPage(mb.build()));

mb.setContent("This is category 2");
categories.put(Emoji.fromFormatted("\u26bd"), new InteractPage(mb.build()));

mb.setContent("This is category 3");
categories.put(Emoji.fromFormatted("\u270f"), new InteractPage(mb.build()));
```

Then just call `Pages.categorize()` method just like you did before:

```java
exampleChannel.sendMessage("This is a menu message").queue(success -> {
	Pages.categorize(success, categories, /* Use buttons? */ true);
});
```

## How do I buttonize?

To do it, you first need to setup a few things:

```java
// A Consumer is a callback function that uses one arguments and returns nothing
// Here, the member is the one that pressed the button, and message is the buttonized message itself
ThrowingConsumer<ButtonWrapper> customFunction = (wrapper) -> {
	// Example of giving a role to anyone who presses this button
	guild.addRoleToMember(wrapper.getMember(), guild.getRoleById("123456789")).queue();
};

exampleChannel.sendMessage("This is a sample message").queue(success -> {
	Pages.buttonize(success, Collections.singletonMap(Emoji.fromFormatted("✅"), customFunction), /* Use buttons? */ true, /* Show cancel? */ true);
});
```

## I have low memory, what about me?

Yet again, don't worry, Pagination-Utils to the rescue!

```java
// Could be anything, this is just an example.
List<String> data = new ArrayList<>();
// Adding 10 values to the list
for (int i = 0; i < 10; i++) {
	data.add("This is entry Nº " + i);
}

MessageBuilder mb = new MessageBuilder();
ThrowingFunction<Integer, Page> func = i -> {
	mb.setContent("This is entry Nº " + i);
	return new InteractPage(mb.build());
};
```

Then just call `Pages.lazyPaginate()` method:

```java
// Second argument must be a function that takes an integer and returns a Page.
// Third argument is whether you want to use reactions (false) or interaction buttons (true).
// The last argument defines whether pages should be cached or not (will keep previously visited pages in memory).
exampleChannel.sendMessage((Message) pages.get(0).getContent()).queue(success -> {
	Pages.lazyPaginate(success, func, /* Use buttons? */ true, /* Cache? */ false);
});
```

## Is it really that easy?

Yes, you can focus on creating epic menus, ranking, lists, whatever and leave the boring part for the library to do its
job, isn't that awesome?

## How do I get it?

This library is available for manual installation and through Jitpack:

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

## Feedback

If you have any issues using this library, feel free to create a new issue and I'll review it as soon as possible!
