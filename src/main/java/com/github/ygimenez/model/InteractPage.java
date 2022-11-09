package com.github.ygimenez.model;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Class representing either a {@link Message} or {@link MessageEmbed} object.
 * Fundamentally the same as {@link Page} but will use {@link Button}s instead of emotes.
 */
public class InteractPage extends Page {
	private final Map<ButtonStyle, ButtonStyle> styles = new EnumMap<>(ButtonStyle.class);
	private final Map<Emote, String> caption = new EnumMap<>(Emote.class);
	private final boolean ephemeral;

	/**
	 * An {@link InteractPage} object to be used in this library's methods. Currently, only {@link Message}
	 * and {@link MessageEmbed} are supported.
	 *
	 * @param content The {@link Message}/{@link MessageEmbed} object to be used as pages.
	 * @throws IllegalArgumentException Thrown if argument is not a {@link Message} nor {@link MessageEmbed}.
	 */
	public InteractPage(@NotNull Object content) throws IllegalArgumentException {
		super(content);
		this.ephemeral = false; //Attribute currently unused
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
	public Button makeButton(@Nonnull Emote emt) {
		ButtonStyle style = styles.getOrDefault(emt.getStyle(), emt.getStyle());

		if (emt == Emote.NONE) {
			return Button.secondary(emt.name() + "." + Objects.hash(Math.random()), "\u200B").asDisabled();
		} else {
			return Button.of(style, (ephemeral ? "*" : "") + emt.name(), caption.get(emt), Pages.getPaginator().getEmote(emt));
		}
	}

	/**
	 * Creates a new {@link Button}, but without any style or caption applied to it.
	 *
	 * @param emj The {@link Emoji} representing the {@link Button}, must never be null since it is also the ID.
	 * @return The created {@link Button}.
	 */
	public Button makeButton(@Nonnull Emoji emj) {
		return Button.secondary((ephemeral ? "*" : "") + ((CustomEmoji) emj).getId(), emj);
	}

	/**
	 * Creates a new {@link Button}, but without any style applied to it.
	 *
	 * @param emj     The {@link Emoji} representing the {@link Button}, must never be null since it is also the ID.
	 * @param caption The desired caption for the {@link Button}.
	 * @return The created {@link Button}.
	 */
	public Button makeButton(@Nonnull Emoji emj, String caption) {
		return Button.of(ButtonStyle.SECONDARY, (ephemeral ? "*" : "") + ((CustomEmoji) emj).getId(), caption, emj);
	}

	/**
	 * Whether the button is intended to be used in ephemeral messages or not. Currently, it serves no purpose other
	 * than a placeholder for future features.
	 *
	 * @return Whether the {@link Button} will be used in ephemeral messages or not.
	 */
	public boolean isEphemeral() {
		return ephemeral;
	}
}
