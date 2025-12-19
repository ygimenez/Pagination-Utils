package com.github.ygimenez.model;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.type.Action;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Class representing either a {@link String}, {@link MessageEmbed} or {@link EmbedCluster} object.
 * Fundamentally the same as {@link Page} but will use {@link Button}s instead of emotes.
 */
public class InteractPage extends Page {
	private final Map<ButtonStyle, ButtonStyle> styles = new EnumMap<>(ButtonStyle.class);
	private final Map<Action, String> caption = new EnumMap<>(Action.class);

	/**
	 * Create a new {@link InteractPage} for embed-less page, with support for interaction buttons.
	 * <b>THIS MUST NEVER BE CALLED.</b>
	 *
	 * @param content The desired content
	 */
	protected InteractPage(@NotNull Object content) {
		super(content);
	}

	/**
	 * Create a new {@link InteractPage} for embed-less page, with support for interaction buttons.
	 *
	 * @param content The desired content
	 * @return A new {@link InteractPage} instance.
	 */
	public static InteractPage of(@NotNull String content) {
		return new InteractPage(content);
	}

	/**
	 * Create a new {@link InteractPage} for single-embed page, with support for interaction buttons.
	 *
	 * @param content The desired content
	 * @return A new {@link InteractPage} instance.
	 */
	public static InteractPage of(@NotNull MessageEmbed content) {
		return new InteractPage(content);
	}

	/**
	 * Create a new {@link InteractPage} for multi-embed page, with support for interaction buttons.
	 *
	 * @param content The desired content
	 * @return A new {@link InteractPage} instance.
	 */
	public static InteractPage of(@NotNull EmbedCluster content) {
		return new InteractPage(content);
	}

	/**
	 * Retrieves current {@link Map} of {@link Button} style overrides.
	 *
	 * @return The {@link Map} of style overrides.
	 */
	public Map<ButtonStyle, ButtonStyle> getStyles() {
		return styles;
	}

	/**
	 * Override a {@link Button} style (for example, making {@link Action#ACCEPT} button become red).
	 *
	 * @param original The original style to be overridden.
	 * @param override The new style.
	 */
	public void overrideStyle(ButtonStyle original, ButtonStyle override) {
		styles.put(original, override);
	}

	/**
	 * Retrieves current {@link Map} of {@link Button} captions.
	 *
	 * @return The {@link Map} of captions.
	 */
	public Map<Action, String> getCaptions() {
		return caption;
	}

	/**
	 * Creates a new {@link Button} from configured styles and captions.
	 *
	 * @param action The {@link Action} representing the {@link Button}, must never be null since it is also the ID.<br>
	 *            If you supply {@link Action#NONE} a blank disabled button will be created.
	 * @return The created {@link Button}.
	 */
	public Button makeButton(@NotNull Action action) {
		ButtonStyle style = styles.getOrDefault(action.getStyle(), ButtonStyle.SECONDARY);
		String key = action.name() + "." + (int) (Math.random() * Integer.MAX_VALUE);

		if (action == Action.NONE) {
			return Button.secondary(key, "\u200B").asDisabled();
		} else {
			return Button.of(style, key, caption.get(action), Pages.getPaginator().getEmoji(action));
		}
	}

	/**
	 * Creates a new {@link Button}, but without any style or caption applied to it.
	 *
	 * @param id The {@link EmojiId} or {@link TextId} representing the {@link Button}. If null, a blank disabled button
	 *           will be created.
	 * @return The created {@link Button}.
	 */
	public Button makeButton(@Nullable ButtonId<?> id) {
		if (id != null) {
			String key = id.getId() + "." + (int) (Math.random() * Integer.MAX_VALUE);

			if (id instanceof TextId) {
				return Button.of(id.getStyle(), key, id.getLabel());
			} else if (id instanceof EmojiId) {
				return Button.of(id.getStyle(), key, id.getLabel(), ((EmojiId) id).getContent());
			}
		}

		return Button.secondary(String.valueOf((int) (Math.random() * Integer.MAX_VALUE)), "\u200B").asDisabled();
	}
}
