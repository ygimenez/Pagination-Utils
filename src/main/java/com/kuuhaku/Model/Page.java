package com.kuuhaku.Model;

import com.kuuhaku.Enum.PageType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import javax.annotation.Nonnull;

public class Page {
	private final PageType type;
	private final Object content;

	/**
	 * A Page object to be used in this library's methods. Currently only Message
	 * and MessageEmbed types are supported.
	 * 
	 * @param type    The type of the content (PageType.TEXT or PageType.EMBED)
	 * @param content The Message/MessageEmbed object to be used as pages
	 */
	public Page(@Nonnull PageType type, @Nonnull Object content) {
		this.type = type;
		this.content = content;
	}

	public PageType getType() {
		return type;
	}

	public Object getContent() {
		return content;
	}

	@Override
	public String toString() {
		if (type == PageType.TEXT) {
			return ((Message) content).getContentRaw();
		} else {
			return ((MessageEmbed) content).getDescription();
		}
	}
}
