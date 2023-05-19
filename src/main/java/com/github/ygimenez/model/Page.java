package com.github.ygimenez.model;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

/**
 * Class representing either a {@link String}, {@link MessageEmbed} or {@link EmbedCluster} object.
 */
public class Page {
	private final Object content;

	protected Page(@NotNull Object content) throws IllegalArgumentException {
		if (!(content instanceof String || content instanceof MessageEmbed || content instanceof EmbedCluster)) {
			throw new IllegalArgumentException("Page content must be either a String or a MessageEmbed");
		}

		this.content = content;
	}

	/**
	 * Create a new {@link Page} for embed-less page, with support for interaction buttons.
	 *
	 * @param content The desired content
	 * @return A new {@link Page} instance.
	 */
	public static Page of(@NotNull String content) {
		return new Page(content);
	}

	/**
	 * Create a new {@link Page} for single-embed page, with support for interaction buttons.
	 *
	 * @param content The desired content
	 * @return A new {@link Page} instance.
	 */
	public static Page of(@NotNull MessageEmbed content) {
		return new Page(content);
	}

	/**
	 * Create a new {@link Page} for multi-embed page, with support for interaction buttons.
	 *
	 * @param content The desired content
	 * @return A new {@link Page} instance.
	 */
	public static Page of(@NotNull EmbedCluster content) {
		return new Page(content);
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
		} else if (content instanceof EmbedCluster) {
			return ((EmbedCluster) content).getEmbeds().stream()
					.map(MessageEmbed::getDescription)
					.collect(Collectors.joining("\n"));
		} else {
			return "Unknown type";
		}
	}
}
