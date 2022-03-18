package com.github.ygimenez.model.helper;

import com.github.ygimenez.model.Page;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;

import java.util.LinkedHashMap;
import java.util.Map;

public class CategorizeHelper extends BaseHelper<CategorizeHelper, Map<Emoji, Page>> {
	public CategorizeHelper(Message message, boolean useButtons) {
		super(CategorizeHelper.class, message, new LinkedHashMap<>(), useButtons);
	}

	public CategorizeHelper(Message message, Map<Emoji, Page> categories, boolean useButtons) {
		super(CategorizeHelper.class, message, categories, useButtons);
	}

	public CategorizeHelper addCategory(Emoji emoji, Page page) {
		getContent().put(emoji, page);
		return this;
	}
}
