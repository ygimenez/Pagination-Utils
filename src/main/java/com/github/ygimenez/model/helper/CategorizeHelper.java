package com.github.ygimenez.model.helper;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.*;
import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.ygimenez.type.Emote.CANCEL;

/**
 * Helper class for building categorize events, safe for reuse.
 */
public class CategorizeHelper extends BaseHelper<CategorizeHelper, Map<ButtonId<?>, Page>> {
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
	public CategorizeHelper(Map<ButtonId<?>, Page> categories, boolean useButtons) {
		super(CategorizeHelper.class, categories, useButtons);
	}

	/**
	 * Creates a new categorize event helper with the supplied mapping. Utility constructor for {@link String}-mapped categories.
	 *
	 * @param categories A {@link TextMapping} containing the initial categories.
	 * @param useButtons Whether to use interaction buttons or legacy reaction-based buttons.
	 */
	public CategorizeHelper(TextMapping<Page> categories, boolean useButtons) {
		this(categories.toMap(), useButtons);
	}

	/**
	 * Creates a new categorize event helper with the supplied mapping. Utility constructor for {@link Emoji}-mapped categories.
	 *
	 * @param categories An {@link EmojiMapping} containing the initial categories.
	 * @param useButtons Whether to use interaction buttons or legacy reaction-based buttons.
	 */
	public CategorizeHelper(@NotNull EmojiMapping<Page> categories, boolean useButtons) {
		this(categories.toMap(), useButtons);
	}

	/**
	 * Adds a new category to the map.
	 *
	 * @param emoji The emoji representing this category.
	 * @param page  The page linked to this category.
	 * @return The {@link CategorizeHelper} instance for chaining convenience.
	 */
	public CategorizeHelper addCategory(@NotNull Emoji emoji, @NotNull Page page) {
		return addCategory(emoji, ButtonStyle.SECONDARY, page);
	}

	/**
	 * Adds a new category to the map.
	 *
	 * @param emoji The emoji representing this category.
	 * @param style The style of this category.
	 * @param page  The page linked to this category.
	 * @return The {@link CategorizeHelper} instance for chaining convenience.
	 */
	public CategorizeHelper addCategory(@NotNull Emoji emoji, @NotNull ButtonStyle style, @NotNull Page page) {
		getContent().put(new EmojiId(emoji, style), page);
		return this;
	}

	/**
	 * Adds a new category to the map.
	 *
	 * @param label The label representing this category.
	 * @param page  The page linked to this category.
	 * @return The {@link CategorizeHelper} instance for chaining convenience.
	 */
	public CategorizeHelper addCategory(@NotNull String label, @NotNull Page page) {
		return addCategory(label, ButtonStyle.SECONDARY, page);
	}

	/**
	 * Adds a new category to the map.
	 *
	 * @param label The label representing this category.
	 * @param style The style of this category.
	 * @param page  The page linked to this category.
	 * @return The {@link CategorizeHelper} instance for chaining convenience.
	 */
	public CategorizeHelper addCategory(@NotNull String label, @NotNull ButtonStyle style, @NotNull Page page) {
		getContent().put(new TextId(label, style), page);
		return this;
	}

	/**
	 * Clear all categories.
	 *
	 * @return The {@link CategorizeHelper} instance for chaining convenience.
	 */
	public CategorizeHelper clearCategories() {
		getContent().clear();
		return this;
	}

	@Override
	public <Out extends MessageRequest<Out>> List<LayoutComponent> getComponents(Out action) {
		if (!isUsingButtons()) return List.of();

		List<LayoutComponent> rows = new ArrayList<>();

		List<ItemComponent> row = new ArrayList<>();
		for (Map.Entry<ButtonId<?>, Page> e : getContent().entrySet()) {
			if (row.size() == 5) {
				rows.add(ActionRow.of(row));
				row = new ArrayList<>();
			}

			InteractPage p = (InteractPage) e.getValue();
			if (p == null) continue;

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

		return rows;
	}

	/**
	 * {@inheritDoc}
	 **/
	@Override
	public boolean shouldUpdate(Message msg) {
		if (!isUsingButtons()) return false;

		Predicate<Set<String>> checks = ids -> !isCancellable() || ids.contains(Emote.getId(Pages.getPaginator().getEmoji(CANCEL)));
		checks = checks.and(ids -> {
			for (ButtonId<?> id : getContent().keySet()) {
				String key;

				if (id instanceof EmojiId) {
					key = Emote.getId(((EmojiId) id).getId());
				} else {
					key = String.valueOf(id.getId());
				}

				if (!ids.contains(key)) return false;
			}

			return true;
		});

		Set<String> ids = msg.getButtons().stream()
				.map(Button::getId)
				.collect(Collectors.toSet());

		return !checks.test(ids);
	}
}
