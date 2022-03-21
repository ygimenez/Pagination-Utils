package com.github.ygimenez.exception;

import com.github.ygimenez.model.Page;
import net.dv8tion.jda.api.entities.Message;

import java.util.List;

/**
 * Exception thrown when trying to paginate an empty {@link List} or action returned a null {@link Page}.
 */
public class NullPageException extends RuntimeException {
	/**
	 * Default constructor.
	 */
	public NullPageException() {
		super("The informed collection does not contain any Page or Page is null");
	}

	/**
	 * Parametrized constructor.
	 * @param msg The {@link Message} where this exception originated from.
	 */
	public NullPageException(Message msg) {
		super(String.format("The informed collection does not contain any Page. Source info: (GUILD %s CHANNEL %s MESSAGE %s)",
				msg.getGuild().getId(),
				msg.getChannel().getId(),
				msg.getId()
		));
	}
}
