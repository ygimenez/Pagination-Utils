[build]: https://github.com/ygimenez/PaginationUtils/tree/master
[jitpack]: https://jitpack.io/#ygimenez/PaginationUtils
[bintray]: https://bintray.com/ygimenez/maven/PaginationUtils/_latestVersion
[license]: https://github.com/ygimenez/PaginationUtils/blob/master/LICENSE
[issue]: https://github.com/ygimenez/PaginationUtils/issues
[build-shield]: https://img.shields.io/github/workflow/status/ygimenez/PaginationUtils/Java%20CI?label=Build
[jitpack-shield]: https://img.shields.io/badge/Download-Jitpack-success
[bintray-shield]: https://api.bintray.com/packages/ygimenez/maven/PaginationUtils/images/download.svg
[license-shield]: https://img.shields.io/github/license/ygimenez/PaginationUtils?color=lightgrey&label=License
[issue-shield]: https://img.shields.io/github/issues/ygimenez/PaginationUtils?label=Issues
[ ![bintray-shield][] ][bintray]
[ ![jitpack-shield][] ][jitpack]
[ ![build-shield][] ][build]
[ ![license-shield][] ][license]
[ ![issue-shield][] ][issue]

<img align="right" src="https://i.imgur.com/ptJkj6t.png" height=225 width=225>

# Pagination Utils - JDA Pagination made easier!

With this library you will be using pages and categories in your bot commands in no time!

## What is a page/category/buttons?

![Pagination Example](https://i.imgur.com/5Cain0U.gif)

![Categorization Example](https://i.imgur.com/AEusZQ1.gif)

![Buttonization Example](https://i.imgur.com/4PBVoTn.gif)

Have you been using a bot and came across one of those two GIFs and thought: "That must've been hard to make." or "I'd like one of those in my bot!". Fear no more, KuuHaKu to the rescue!

## How do I paginate?

It's easier than it seems! The first step is to set a JDA client object as the event handler, which can be done in a single line:

```java
JDA bot = ... //CREATION OF THE BOT CLIENT

Pages.activate(bot);
```

Then all you need to do is create a `Page` object containing the type of the content and the `Message`/`MessageEmbed` object that you just created.

Example:

```java
//EXAMPLE USING MESSAGE BUILDER
MessageBuilder mb = new MessageBuilder();
mb.setContent("Hello World!");

Page messagePage = new Page(PageType.TEXT, mb.build());

//EXAMPLE USING EMBED BUILDER
EmbedBuilder eb = new EmbedBuilder();
eb.setTitle("Example Embed");
eb.setDescription("Hello World!");

Page embedPage = new Page(PageType.EMBED, eb.build());
```

That said, you'll need to create an `ArrayList` of pages to use the pagination, since a single `Page` does not need to be paginated at all:

```java
ArrayList<Page> pages = new ArrayList<>();
MessageBuilder mb = new MessageBuilder();

//ADDING 10 PAGES TO THE LIST
for (int i = 0; i < 10; i++) {
    mb.clear();
    mb.setContent("This is entry Nº " + i);
    pages.add(new Page(PageType.TEXT), mb.build());
}
```

Then all you have to do is call `Pages.paginate()` method:

```java
//THIS METHOD REQUIRES 4 ARGUMENTS:
//THE TARGET MESSAGE (Message)
//THE LIST OF PAGES (any List object)
//THE IDLE TIME BEFORE SHUTTING DOWN (int)
//THE TimeUnit FOR THE TIME
exampleChannel.sendMessage((Message) pages.get(0).getContent()).queue(success -> {
    Pages.paginate(success, pages, 60, TimeUnit.SECONDS);
});
```

That's everything you have to do, the library will automatically add the navigation buttons to the target message, which will change its content based on the list's order.

## How do I categorize?

To categorize it's almost the same process as paginating, however, the type of collection is `HashMap` instead of `ArrayList`:

```java
HashMap<String, Page> pages = new HashMap<>();
MessageBuilder mb = new MessageBuilder();

//MANUALLY ADDING 3 CATEGORIES TO THE MAP, YOU COULD USE SOME KIND OF ITERATION TO FILL IT (Map key must be a emoji's unicode or emote name - See https://emojipedia.org/ for unicodes)
mb.setContent("This is category 1");
pages.put("\u26f3", new Page(PageType.TEXT, mb.build()));

mb.setContent("This is category 2");
pages.put("\u26bd", new Page(PageType.TEXT, mb.build()))

mb.setContent("This is category 3");
pages.put("\u270f", new Page(PageType.TEXT, mb.build()))
```

Then just call the `Pages.categorize()` method just like you did with `Pages.paginate()` method:

```java
//SAME ARGUMENTS, EXCEPT THE SECOND THAT MUST EXTEND Map CLASS
exampleChannel.sendMessage("This is a menu message").queue(success -> {
    Pages.categorize(success, pages, 60, TimeUnit.SECONDS);
});
```

## \*NEW\* How do I buttonize?

A new feature in version 1.0.3, is that you're able to add buttons with custom functions using this library. To do it, you first need to setup a few things:

```java
//THE BICONSUMER IS A CALLBACK FUNCTION THAT USES TWO ARGUMENTS INSTEAD OF ONE
//HERE, THE MEMBER IS THE ONE THAT PRESSED THE BUTTON, AND MESSAGE IS THE BUTTONIZED MESSAGE ITSELF
BiConsumer<Member, Message> customFunction = (mb, ms) -> {
    //EXAMPLE OF GIVING A ROLE TO ANYONE WHO PRESSES THIS BUTTON
    guild.addRoleToMember(mb, guild.getRoleById("123456789")).queue();
};

exampleChannel.sendMessage("This is a sample message").queue(success -> {
    //SAME ARUMENTS, EXCEPT THE SECOND THAT MUST EXTEND Map CLASS
    //THE LAST ARGUMENT DEFINES WHETHER TO SHOW CANCEL BUTTON OR NOT
    Pages.buttonize(success, Collections.singletonMap("✅", customFunction), false);
});
```

## Is it really that easy?

Yes, you can focus on creating epic menus, ranking, lists, whatever and leave the boring part for the library to do its job, isn't that awesome?

## How do I get it?

This library is available for manual installation and through Jitpack:

### To install manually
* Click on the releases tab on the top of this repository
* Download the latest release
* Put the .jar file somewhere in your project
* Add it to the buildpath
* Done!

### To install via Jitpack
* Add the Jitpack repository to your project:

Gradle:

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

Maven:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

* Add this library as a dependency:

Gradle:

```gradle
dependencies {
    implementation 'com.github.ygimenez:PaginationUtils:VERSION'
}
```

Maven:

```xml
<dependency>
    <groupId>com.github.ygimenez</groupId>
    <artifactId>PaginationUtils</artifactId>
    <version>VERSION</version>
</dependency>
```

* Replace `VERSION` with the one shown here: [ ![bintray-shield][] ][bintray]
* Done!

## Feedback

If you have any issues using this library, feel free to create a new issue on this repository and I'll review it as soon as possible!
