![Version](https://jitpack.io/v/ygimenez/PaginationUtils.svg)

# Pagination Utils - JDA Pagination made easier!

With this library you will be using pages and categories in your bot commands in no time!

## What is a page/category?

![Demo1](https://i.imgur.com/7bMGoJC.gif)

![Demo2](https://i.imgur.com/ZbgoADy.gif)

Have you been using a bot and came across one of those two GIFs and thought: "That must've been hard to make." or "I'd like one of those in my bot!". Fear no more, KuuHaKu to the rescue!

## How do I paginate?

It's easier than it seems, all you need to do is create a `Page` object containing the type of the content and the `Message`/`MessageEmbed` object that you just created.

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
//THIS METHOD REQUIRES 4 VARIABLES:
//THE BOT'S OBJECT (JDA)
//THE TARGET MESSAGE (Message)
//THE LIST OF PAGES (any List object)
//THE IDLE TIME BEFORE SHUTTING DOWN (int)
//THE TimeUnit FOR THE TIME
exampleChannel.sendMessage((Message) pages.get(0).getContent()).queue(success -> {
    Pages.paginate(bot, success, pages, 60, TimeUnit.SECONDS);
});
```

That's everything you have to do, the library will automatically add the navigation buttons to the target message, which will change its content based on the list's order.

## How do I categorize?

To categorize it's almost the same process as paginating, however, the type of collection is `HashMap` instead of `ArrayList`:

```java
HashMap<String, Page> pages = new HashMap<>();
MessageBuilder mb = new MessageBuilder();

//MANUALLY ADDING 3 CATEGORIES TO THE MAP, YOU COULD USE SOME KIND OF ITERATION TO FILL IT (Map key must be a emote's unicode - See https://emojipedia.org/ for unicodes)
mb.setContent("This is category 1");
pages.put("\u26f3", new Page(PageType.TEXT, mb.build()));

mb.setContent("This is category 2");
pages.put("\u26bd", new Page(PageType.TEXT, mb.build()))

mb.setContent("This is category 3");
pages.put("\u270f", new Page(PageType.TEXT, mb.build()))
```

Then just call the `Pages.categorize()` method just like you did with `Pages.paginate()` method:

```java
//SAME ARGUMENTS, EXCEPT THE THIRD THAT MUST EXTEND Map CLASS
exampleChannel.sendMessage("This is a menu message").queue(success -> {
    Pages.categorize(bot, success, pages, 60, TimeUnit.SECONDS);
});
```

## Is it really that easy?

Yes, you can focus on creating epic menus, raking, lists, whatever and leave the boring part for the library to do its job, isn't that awesome?

## How do I get it?

This library is available for manual instalation and through Jitpack:

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

* Replace `VERSION` with the one shown here: ![Version](https://jitpack.io/v/ygimenez/PaginationUtils.svg)
* Done!

## Feedback

If you have any issue using this library, feel free to create a new issue on this repository, I'll review it as soon as possible!
