package com.github.ygimenez.model;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Class representing either a {@link String}, {@link MessageEmbed} or {@link EmbedCluster} object.
 * Fundamentally the same as {@link Page} but will use {@link Button}s instead of emotes.
 */
public class InteractPage extends Page {
	private final Map<ButtonStyle, ButtonStyle> styles = new EnumMap<>(ButtonStyle.class);
	private final Map<Emote, String> caption = new EnumMap<>(Emote.class);

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
	 * Override a {@link Button} style (for example, making {@link Emote#ACCEPT} button become red).
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
	public Map<Emote, String> getCaptions() {
		return caption;
	}

	/**
	 * Creates a new {@link Button} from configured styles and captions.
	 *
	 * @param emt The {@link Emote} representing the {@link Button}, must never be null since it is also the ID.<br>
	 *            If you supply {@link Emote#NONE} a blank disabled button will be created.
	 * @return The created {@link Button}.
	 */
	public Button makeButton(@NotNull Emote emt) {
		ButtonStyle style = styles.getOrDefault(emt.getStyle(), ButtonStyle.SECONDARY);

		if (emt == Emote.NONE) {
			return Button.secondary(emt.name() + "." + Objects.hash(Math.random()), "\u200B").asDisabled();
		} else {
			return Button.of(style, emt.name(), caption.get(emt), Pages.getPaginator().getEmoji(emt));
		}
	}

	/**
	 * Creates a new {@link Button}, but without any style or caption applied to it.
	 *
	 * @param id The {@link EmojiId} or {@link TextId} representing the {@link Button}, must never be null since it is also the ID.
	 * @return The created {@link Button}.
	 */
	public Button makeButton(@NotNull ButtonId<?> id) {
		if (id instanceof EmojiId) {
			return Button.secondary(id.extractId(), ((EmojiId) id).getId());
		}

		String key = id.extractId();
		return Button.secondary(key, key);
	}
}
