package com.github.ygimenez.model.helper;

import com.github.ygimenez.model.Page;
import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.List;

public class PaginateHelper extends BaseHelper<PaginateHelper, List<Page>> {
	private int skipAmount = 0;
	private boolean fastForward = false;

	public PaginateHelper(Message message, boolean useButtons) {
		super(PaginateHelper.class, message, new ArrayList<>(), useButtons);
	}

	public PaginateHelper(Message message, List<Page> pages, boolean useButtons) {
		super(PaginateHelper.class, message, pages, useButtons);
	}

	public PaginateHelper addPage(Page page) {
		getContent().add(page);
		return this;
	}

	public int getSkipAmount() {
		return skipAmount;
	}

	public PaginateHelper setSkipAmount(int skipAmount) {
		this.skipAmount = skipAmount;
		return this;
	}

	public boolean isFastForward() {
		return fastForward;
	}

	public PaginateHelper setFastForward(boolean fastForward) {
		this.fastForward = fastForward;
		return this;
	}
}
