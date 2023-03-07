package com.github.ygimenez.model.helper;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageRequest;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.ygimenez.type.Emote.CANCEL;

/**
 * Helper class for building categorize events, safe for reuse.
 */
public class CategorizeHelper extends BaseHelper<CategorizeHelper, Map<Emoji, Page>> {
	/**
	 * Creates a new categorize event helper with the default map implementation ({@link LinkedHashMap}).
	 *
	 * @param useButtons Whether to use interaction buttons or legacy reaction-based buttons.
	 */
	public CategorizeHelper(boolean useButtons) {
		super(CategorizeHelper.class, new LinkedHashMap<>(), useButtons);
	}

	/**
	 * Creates a new categorize event helper with the supplied map.
	 *
	 * @param categories A map containing the initial categories.
	 * @param useButtons Whether to use interaction buttons or legacy reaction-based buttons.
	 */
	public CategorizeHelper(Map<Emoji, Page> categories, boolean useButtons) {
		super(CategorizeHelper.class, categories, useButtons);
	}

	/**
	 * Adds a new category to the map.
	 *
	 * @param emoji The emoji representing this category.
	 * @param page The page linked to this category.
	 * @return The {@link CategorizeHelper} instance for chaining convenience.
	 */
	public CategorizeHelper addCategory(Emoji emoji, Page page) {
		getContent().put(emoji, page);
		return this;
	}

	/** {@inheritDoc} **/
	@Override
	public <Out extends MessageRequest<Out>> Out apply(Out action) {
		if (!isUsingButtons()) return action;

		List<ActionRow> rows = new ArrayList<>();

		List<ItemComponent> row = new ArrayList<>();
		for (Map.Entry<Emoji, Page> e : getContent().entrySet()) {
			if (row.size() == 5) {
				rows.add(ActionRow.of(row));
				row = new ArrayList<>();
			}

			InteractPage p = (InteractPage) e.getValue();
			Button b = p.makeButton(e.getKey());
			if (p.getContent() instanceof MessageEmbed) {
				for (MessageEmbed embed : action.getEmbeds()) {
					if (embed.equals(p.getContent())) {
						b = b.asDisabled();
						break;
					}
				}
			} else if (action.getContent().equals(p.getContent())) {
				b = b.asDisabled();
			}

			row.add(b);
		}

		if (isCancellable()) {
			Button button = Button.danger(CANCEL.name(), Pages.getPaginator().getEmoji(CANCEL));
			if (rows.size() == 5 && row.size() == 5) {
				row.set(4, button);
			} else if (row.size() == 5) {
				rows.add(ActionRow.of(row));
				row = new ArrayList<>();

				row.add(button);
			} else {
				row.add(button);
			}
		}

		rows.add(ActionRow.of(row));

		return action.setComponents(rows);
	}

	/** {@inheritDoc} **/
	@Override
	public boolean shouldUpdate(Message msg) {
		if (!isUsingButtons()) return false;

		Predicate<Set<Emoji>> checks = e -> !isCancellable() || e.contains(Pages.getPaginator().getEmoji(CANCEL));
		Set<Emoji> emojis = msg.getButtons().stream()
				.map(Button::getEmoji)
				.collect(Collectors.toSet());

		checks = checks.and(e -> e.containsAll(getContent().keySet()));

		return !checks.test(emojis);
	}
}
