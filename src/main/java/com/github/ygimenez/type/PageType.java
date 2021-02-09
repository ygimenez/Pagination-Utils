package com.github.ygimenez.type;

import com.github.ygimenez.model.Page;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

/**
 * The type of the object supplied to {@link Page}.
 */
public enum PageType {
	/**
	 * {@link Message}-type {@link Page}.
	 */
	TEXT,
	/**
	 * {@link MessageEmbed}-type {@link Page}.
	 */
	EMBED
}
