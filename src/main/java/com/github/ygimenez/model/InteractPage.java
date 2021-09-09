package com.github.ygimenez.model;

import com.github.ygimenez.type.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * Class representing either a {@link Message} or {@link MessageEmbed} object.
 * Fundamentally the same as {@link Page} but will use {@link Button}s instead of emotes.
 */
public class InteractPage extends Page {
	private final Map<ButtonStyle, ButtonStyle> styles = new EnumMap<>(ButtonStyle.class);
	private final Map<Emote, String> caption = new EnumMap<>(Emote.class);

	/**
	 * An {@link InteractPage} object to be used in this library's methods. Currently, only {@link Message}
	 * and {@link MessageEmbed} are supported.
	 *
	 * @param content The {@link Message}/{@link MessageEmbed} object to be used as pages.
	 * @throws IllegalArgumentException Thrown if argument is not a {@link Message} nor {@link MessageEmbed}.
	 */
	public InteractPage(@NotNull Object content) throws IllegalArgumentException {
		super(content);
	}

	public Map<ButtonStyle, ButtonStyle> getStyles() {
		return styles;
	}

	public void overrideStyle(ButtonStyle original, ButtonStyle override) {
		styles.put(original, override);
	}

	public Map<Emote, String> getCaptions() {
		return caption;
	}

	public Button makeButton(Paginator pag, Emote emt) {
		ButtonStyle style = styles.getOrDefault(emt.getStyle(), emt.getStyle());

		if (emt == Emote.NONE) {
			return Button.secondary(emt.name() + System.currentTimeMillis(), "").asDisabled();
		} else {
			return Button.of(style, emt.name(), caption.get(emt), pag.getEmote(emt));
		}
	}
}
