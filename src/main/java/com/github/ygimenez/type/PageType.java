package com.github.ygimenez.type;

import com.github.ygimenez.model.Page;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

/**
 * <strong>DEPRECATED:</strong> This enum is redundant for newest versions (will be removed in version 2.5.0).<br>
 * <br>
 * The type of the object supplied to {@link Page}.
 */
@Deprecated(since = "2.2.0", forRemoval = true)
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
