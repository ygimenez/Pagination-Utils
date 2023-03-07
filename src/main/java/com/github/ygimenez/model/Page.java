package com.github.ygimenez.model;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

/**
 * Class representing either a {@link String} or {@link MessageEmbed} object.
 */
public class Page {
	private final Object content;

	/**
	 * A {@link Page} object to be used in this library's methods. Currently, only {@link String}
	 * and {@link MessageEmbed} are supported.
	 * 
	 * @param content The {@link String}/{@link MessageEmbed} object to be used as pages.
	 * @throws IllegalArgumentException Thrown if argument is not a {@link String} nor {@link MessageEmbed}.
	 */
	public Page(@NotNull Object content) throws IllegalArgumentException {
		if (!(content instanceof String) && !(content instanceof MessageEmbed)) {
			throw new IllegalArgumentException("Page content must be either a String or a MessageEmbed");
		}

		this.content = content;
	}

	/**
	 * Method to get this {@link Page}'s content object.
	 *
	 * @return This {@link Page}'s content object.
	 */
	public Object getContent() {
		return content;
	}

	/**
	 * Method to get this {@link Page}'s main {@link String} content ({@link Message} content or {@link MessageEmbed} description).
	 *
	 * @return This {@link Page}'s main {@link String} content.
	 */
	@Override
	public String toString() {
		if (content instanceof Message) {
			return ((Message) content).getContentRaw();
		} else if (content instanceof MessageEmbed) {
			return ((MessageEmbed) content).getDescription();
		} else {
			return "Unknown type";
		}
	}
}
