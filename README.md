[build]: https://github.com/ygimenez/PaginationUtils/tree/master

[jitpack]: https://jitpack.io/#ygimenez/PaginationUtils

[mvncentral]: https://mvnrepository.com/artifact/com.github.ygimenez/Pagination-Utils

[license]: https://github.com/ygimenez/PaginationUtils/blob/master/LICENSE

[issue]: https://github.com/ygimenez/PaginationUtils/issues

[build-shield]: https://img.shields.io/github/workflow/status/ygimenez/PaginationUtils/Java%20CI?label=Build

[jitpack-shield]: https://img.shields.io/badge/Download-Jitpack-success

[mvncentral-shield]: https://img.shields.io/maven-central/v/com.github.ygimenez/Pagination-Utils?label=Maven%20Central

[license-shield]: https://img.shields.io/github/license/ygimenez/PaginationUtils?color=lightgrey&label=License

[issue-shield]: https://img.shields.io/github/issues/ygimenez/PaginationUtils?label=Issues
[ ![mvncentral-shield][] ][mvncentral]
[ ![jitpack-shield][] ][jitpack]
[ ![build-shield][] ][build]
[ ![license-shield][] ][license]
[ ![issue-shield][] ][issue]

<img style="float: right" src="https://raw.githubusercontent.com/ygimenez/Pagination-Utils/master/icon.png" height=225 width=225 alt="Pagination Utils logo">

# Pagination Utils - JDA Pagination made easier!

With this library you will be using pages and categories in your bot commands in no time!

## What is a page/category/button?

![Pagination Example](https://i.imgur.com/5Cain0U.gif)

![Categorization Example](https://i.imgur.com/AEusZQ1.gif)

![Buttonization Example](https://i.imgur.com/4PBVoTn.gif)

Have you been using a bot and came across one of those three GIFs and thought: "That must've been hard to make." or "I'd
like one of those in my bot!". Fear no more, KuuHaKu to the rescue!

## How do I paginate?

Before we start the fun stuff, there're a few things we need to check:

- You're using Java JDK 9 or above.
- Your bot has the following permissions:
	- MESSAGE_ADD_REACTION
	- MESSAGE_EXT_EMOJI
	- MESSAGE_READ/WRITE
	- VIEW_CHANNEL
- If using `JDABuilder.createLight()`, you added the following gateway intents:
	- GUILD_MESSAGES
	- GUILD_MESSAGE_REACTIONS

Now we can finally start, which is easier than it seems! The first step is to set a JDA client object as the event
holder, which can be done in a single line:

```java
JDA bot = ... //Creation of bot client
		
Pages.activate(PaginatorBuilder.createSimplePaginator(bot));
```

But if you want some customization of the library's behaviour, you can opt to use the full builder:

```java
JDA bot = ... //Creation of bot client

Paginator paginator = PaginatorBuilder.createPaginator()
		//Defines which handler will be used
		.setHandler(bot)
		//Whether reactions will be removed on click
		.shouldRemoveOnReaction(false)
		//Finish configuration and activate the library
		.activate();
```

If you want to go even further and change the default buttons' emotes, don't worry, we got you covered:

```java
JDA bot = ... //Creation of bot client

Paginator paginator = PaginatorBuilder.createPaginator()
		//Defines which handler will be used
		.setHandler(bot)
		//Whether reactions will be removed on click
		.shouldRemoveOnReaction(false)
		//Changes the next button to 😙
		.setEmote(Emote.NEXT, Emoji.fromMarkdown("😙"))
		//Changes the previous button to 😩
		.setEmote(Emote.PREVIOUS, Emoji.fromMarkdown("😩"))
		//Finish configuration and activate the library
		.activate();
```

Then all you need to do is create a `Page` collection containing the type of the content and the `Message`/`MessageEmbed` object that you just created.

Example:

```java
//Example using MessageBuilder
MessageBuilder mb = new MessageBuilder();
mb.setContent("Hello World!");

Page messagePage = new Page(mb.build());

//Example using EmbedBuilder
EmbedBuilder eb = new EmbedBuilder();
eb.setTitle("Example Embed");
eb.setDescription("Hello World!");

Page embedPage = new Page(eb.build());
```

That said, you'll need to create an `ArrayList` of pages to use the pagination, since a single `Page` does not need to be paginated at all:

```java
ArrayList<Page> pages = new ArrayList<>();
MessageBuilder mb = new MessageBuilder();

//Adding 10 pages to the list
for (int i = 0; i < 10; i++) {
	mb.clear();
	mb.setContent("This is entry Nº " + i);
	pages.add(new Page(mb.build()));
}
```

Then all you have to do is call `Pages.paginate()` method:

```java
//This method requires 2 arguments:
//The target message
//The list of pages
exampleChannel.sendMessage((Message) pages.get(0).getContent()).queue(success -> {
	Pages.paginate(success,pages);
});
```

That's everything you have to do, the library will automatically add the navigation buttons to the target message, which
will change its content based on the list's order.

## How do I categorize?

To categorize it's almost the same process as paginating, however, the type of collection is `HashMap` instead
of `ArrayList`:

```java
HashMap<String, Page> categories = new HashMap<>();
MessageBuilder mb = new MessageBuilder();

//Manually adding 3 categories to the map, you could use some kind of loop to fill it (see https://emojipedia.org/ for unicodes)
mb.setContent("This is category 1");
categories.put(Emoji.fromMarkdown("\u26f3"), new Page(mb.build()));

mb.setContent("This is category 2");
categories.put(Emoji.fromMarkdown("\u26bd"), new Page(mb.build()));

mb.setContent("This is category 3");
categories.put(Emoji.fromMarkdown("\u270f"), new Page(mb.build()));
```

Then just call `Pages.categorize()` method just like you did before:

```java
exampleChannel.sendMessage("This is a menu message").queue(success -> {
	Pages.categorize(success, categories);
});
```

## How do I buttonize?

To do it, you first need to setup a few things:

```java
//A BiConsumer is a callback function that uses two arguments instead of one
//Here, the member is the one that pressed the button, and message is the buttonized message itself
ThrowingBiConsumer<Member, Message> customFunction = (mb, ms) -> {
	//Example of giving a role to anyone who presses this button
	guild.addRoleToMember(mb, guild.getRoleById("123456789")).queue();
};

exampleChannel.sendMessage("This is a sample message").queue(success -> {
	//Same aruments, except the second that must extend map collection
	//The last argument defines whether to show cancel button or not
	Pages.buttonize(success, Collections.singletonMap(Emoji.fromMarkdown("✅"), customFunction), false);
});
```

## What if I want categories with pages?

Don't worry, we gotcha!

```java
List<HashMap<Emoji, Page>> pages = new HashMap<>();
MessageBuilder mb = new MessageBuilder();

for (int i = 0; i < 5; i++) {
	HashMap<Emoji, Page> categories = new HashMap<>();

	//Manually adding 3 categories to the map, you could use some kind of loop to fill it (see https://emojipedia.org/ for unicodes)
	mb.setContent("This is category " + ((i * 3) + 1));
	categories.put(Emoji.fromMarkdown("\u26f3"), new Page(mb.build()));

	mb.setContent("This is category " + ((i * 3) + 2));
	categories.put(Emoji.fromMarkdown("\u26bd"), new Page(mb.build()));

	mb.setContent("This is category " + ((i * 3) + 3));
	categories.put(Emoji.fromMarkdown("\u270f"), new Page(mb.build()));

	pages.add(categories);
}
```

Then just call `Pages.paginoCategorize()` as always:

```java
//You can supply a list of "faces" in the third argument which will define the landing page of each category.
exampleChannel.sendMessage("This is a menu message").queue(success -> {
	Pages.paginoCategorize(success, categories, null);
});
```

## I have low memory, what about me?

Yet again, don't worry, Pagination-Utils to the rescue!

```java
//Could be anything, this is just an example.
List<String> data = new ArrayList<>();
//Adding 10 values to the list
for (int i = 0; i < 10; i++) {
	data.add("This is entry Nº " + i);
}

MessageBuilder mb = new MessageBuilder();
ThrowingFunction<Integer, Page> func = i -> {
	mb.setContent("This is entry Nº " + i);
	return new Page(mb.build());
};
```

Then just call `Pages.lazyPaginate()` method:

```java
//Second argument must be a function that takes an integer and returns a Page.
//For third parameter, setting to true will enable page caching (will keep previously visited pages in memory).
exampleChannel.sendMessage((Message) pages.get(0).getContent()).queue(success -> {
	Pages.lazyPaginate(success, func, false);
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
repositories {
    maven { url 'https://jitpack.io' }
}
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

If you have any issues using this library, feel free to create a new issue on this repository and I'll review it as soon
as possible!
