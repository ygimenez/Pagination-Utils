package com.github.ygimenez.model;

import com.github.ygimenez.type.PageType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import javax.annotation.Nonnull;

public class Page {
	private final PageType type;
	private final Object content;

	/**
	 * A {@link Page} object to be used in this library's methods. Currently only {@link Message}
	 * and {@link MessageEmbed} types are supported.
	 * 
	 * @param type    The type of the content ({@link PageType#TEXT} or {@link PageType#EMBED})
	 * @param content The {@link Message}/{@link MessageEmbed} object to be used as pages
	 */
	public Page(@Nonnull PageType type, @Nonnull Object content) {
		this.type = type;
		this.content = content;
	}

	/**
	 * Method to get the {@link Page}'s set type.
	 *
	 * @return This {@link Page}'s type
	 */
	public PageType getType() {
		return type;
	}

	/**
	 * Method to get this {@link Page}'s content object.
	 *
	 * @return This {@link Page}'s content object
	 */
	public Object getContent() {
		return content;
	}

	/**
	 * Method to get this {@link Page}'s main {@link String} content ({@link Message} content for {@link PageType#TEXT} or description for {@link PageType#EMBED})
	 *
	 * @return This {@link Page}'s main {@link String} content
	 */
	@Override
	public String toString() {
		if (type == PageType.TEXT) {
			return ((Message) content).getContentRaw();
		} else {
			return ((MessageEmbed) content).getDescription();
		}
	}
}
