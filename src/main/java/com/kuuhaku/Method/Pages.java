package kuuhaku.Method;


import kuuhaku.Enum.PageType;
import kuuhaku.Listener.MessageListener;
import kuuhaku.Model.Page;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static kuuhaku.Enum.Emote.*;

public class Pages {

	/**
	 * @param api The bot's instantiated object.
	 * @param msg The message sent which will be paginated.
	 * @param pages The pages to be shown. The order of the array will define the order of the pages.
	 * @param time The time before the listener automatically stop listening for further events. (Recommended: 60)
	 * @param unit The time's time unit. (Recommended: TimeUnit.SECONDS)
	 */
	public static void paginate(JDA api, Message msg, List<Page> pages, int time, TimeUnit unit) {
		try {
			msg.addReaction(PREVIOUS.getCode()).queue();
			msg.addReaction(CANCEL.getCode()).queue();
			msg.addReaction(NEXT.getCode()).queue();
			api.addEventListener(new MessageListener() {
				private final int maxP = pages.size() - 1;
				private int p = 0;
				private Future<?> timeout;
				private final Consumer<Void> success = s -> api.removeEventListener(this);

				@Override
				public void onGenericMessageReaction(@Nonnull GenericMessageReactionEvent event) {
					if (timeout == null) timeout = msg.clearReactions().queueAfter(time, unit, success);
					if (event.getUser().isBot()) return;

					timeout.cancel(true);
					timeout = msg.clearReactions().queueAfter(time, unit, success);
					if (event.getReactionEmote().getName().equals(PREVIOUS.getCode())) {
						if (p > 0) {
							p--;
							Page pg = pages.get(p);

							updatePage(msg, pg);
						}
					} else if (event.getReactionEmote().getName().equals(NEXT.getCode())) {
						if (p < maxP) {
							p++;
							Page pg = pages.get(p);

							updatePage(msg, pg);
						}
					} else if (event.getReactionEmote().getName().equals(CANCEL.getCode())) {
						msg.clearReactions().queue(success);
					}
				}

				@Override
				public void onMessageDelete(@Nonnull MessageDeleteEvent event) {
					if (event.getMessageId().equals(msg.getId())) {
						timeout.cancel(true);
						timeout = null;
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param api The bot's instantiated object.
	 * @param msg The message sent which will be categorized.
	 * @param categories The categories to be shown. The categories are defined by a Map containing emote unicodes as keys and Pages as values.
	 * @param time The time before the listener automatically stop listening for further events. (Recommended: 60)
	 * @param unit The time's time unit. (Recommended: TimeUnit.SECONDS)
	 */
	public static void categorize(JDA api, Message msg, Map<String, Page> categories, int time, TimeUnit unit) {
		try {
			categories.keySet().forEach(k -> msg.addReaction(k).queue());
			msg.addReaction(CANCEL.getCode()).queue();
			api.addEventListener(new MessageListener() {
				private String currCat = "";
				private Future<?> timeout;
				private final Consumer<Void> success = s -> api.removeEventListener(this);

				@Override
				public void onGenericMessageReaction(@Nonnull GenericMessageReactionEvent event) {
					if (timeout == null) timeout = msg.clearReactions().queueAfter(time, unit, success);

					if (event.getUser().isBot() || event.getReactionEmote().getName().equals(currCat)) return;
					else if (event.getReactionEmote().getName().equals(CANCEL.getCode())) {
						msg.clearReactions().queue(s -> api.removeEventListener(this));
						return;
					}

					timeout.cancel(true);
					timeout = msg.clearReactions().queueAfter(time, unit, success);

					Page pg = categories.get(event.getReactionEmote().getName());

					currCat = updateCategory(event, msg, pg);
				}

				@Override
				public void onMessageDelete(@Nonnull MessageDeleteEvent event) {
					if (event.getMessageId().equals(msg.getId())) {
						timeout.cancel(true);
						timeout = null;
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void updatePage(Message msg, Page p) {
		if (p.getType() == PageType.TEXT) {
			msg.editMessage((Message) p.getContent()).queue();
		} else {
			msg.editMessage((MessageEmbed) p.getContent()).queue();
		}
	}

	private static String updateCategory(GenericMessageReactionEvent event, Message msg, Page p) {
		AtomicReference<String> out = new AtomicReference<>("");

		if (p.getType() == PageType.TEXT) {
			msg.editMessage((Message) p.getContent()).queue(s -> out.set(event.getReactionEmote().getName()));
		} else {
			msg.editMessage((MessageEmbed) p.getContent()).queue(s -> out.set(event.getReactionEmote().getName()));
		}

		return out.get();
	}
}
