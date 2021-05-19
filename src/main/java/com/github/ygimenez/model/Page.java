package com.github.ygimenez.model;

import com.github.ygimenez.type.PageType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import javax.annotation.Nonnull;

/**
 * Class representing either a {@link Message} or {@link MessageEmbed} object.
 */
public class Page {
	private PageType type;
	private final Object content;

	/**
	 * A {@link Page} object to be used in this library's methods. Currently only {@link Message}
	 * and {@link MessageEmbed} are supported.
	 * 
	 * @param content The {@link Message}/{@link MessageEmbed} object to be used as pages.
	 * @throws IllegalArgumentException Thrown if argument is not a {@link Message} nor {@link MessageEmbed}.
	 */
	public Page(@Nonnull Object content) throws IllegalArgumentException {
		if (!(content instanceof Message) && !(content instanceof MessageEmbed))
			throw new IllegalArgumentException("Page content must be either a Message or a MessageEmbed");

		this.content = content;
	}

	/**
	 * <strong>DEPRECATED:</strong> Please use {@link #Page(Object)} instead (will be removed in version 2.5.0).<br>
	 * <br>
	 * A {@link Page} object to be used in this library's methods. Currently only {@link Message}
	 * and {@link MessageEmbed} types are supported.
	 *
	 * @param type    The type of the content ({@link PageType#TEXT} or {@link PageType#EMBED})
	 * @param content The {@link Message}/{@link MessageEmbed} object to be used as pages
	 */
	@Deprecated(since = "2.2.0", forRemoval = true)
	public Page(@Nonnull PageType type, @Nonnull Object content) {
		this.type = type;
		this.content = content;
	}

	/**
	 * <strong>DEPRECATED:</strong> Please use check {@link Class} instead (will be removed in version 2.5.0).<br>
	 * <br>
	 * Method to get the {@link Page}'s set type.
	 *
	 * @return This {@link Page}'s type
	 */
	@Deprecated(since = "2.2.0", forRemoval = true)
	public PageType getType() {
		return type;
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
