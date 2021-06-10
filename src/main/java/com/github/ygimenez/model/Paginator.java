package com.github.ygimenez.model;

import com.github.ygimenez.type.ButtonOp;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

import java.util.*;

import static com.github.ygimenez.type.ButtonOp.*;

public class Paginator {
	private final HashMap<String, String> emoteCache = new HashMap<>();
	private Object handler = null;
	private boolean removeOnReact = false;
	private boolean eventLocked = false;
	private boolean deleteOnCancel = false;
	private Map<ButtonOp, Style> emotes = new HashMap<>() {{
		put(ACCEPT, Style.of("\u2705", null, ButtonStyle.SUCCESS));
		put(CANCEL, Style.of("\u274E", null, ButtonStyle.DANGER));
		put(NEXT, Style.of("\u25B6", null, ButtonStyle.SECONDARY));
		put(PREVIOUS, Style.of("\u25C0", null, ButtonStyle.SECONDARY));
		put(SKIP_FORWARD, Style.of("\u23E9", null, ButtonStyle.SECONDARY));
		put(SKIP_BACKWARD, Style.of("\u23EA", null, ButtonStyle.SECONDARY));
		put(GOTO_FIRST, Style.of("\u23EE\uFE0F", null, ButtonStyle.SECONDARY));
		put(GOTO_LAST, Style.of("\u23ED\uFE0F", null, ButtonStyle.SECONDARY));
	}};

	protected Paginator() {
	}
	
	protected Paginator(Object handler) {
		this.handler = handler;
	}

	public HashMap<String, String> getEmoteCache() {
		return emoteCache;
	}

	public Object getHandler() {
		return handler;
	}
	
	protected void setHandler(Object handler) {
		this.handler = handler;
	}

	public boolean isRemoveOnReact() {
		return removeOnReact;
	}

	protected void setRemoveOnReact(boolean removeOnReact) {
		this.removeOnReact = removeOnReact;
	}

	public boolean isEventLocked() {
		return eventLocked;
	}

	protected void setEventLocked(boolean hashLocking) {
		this.eventLocked = hashLocking;
	}

	public boolean isDeleteOnCancel() {
		return deleteOnCancel;
	}

	protected void setDeleteOnCancel(boolean deleteOnCancel) {
		this.deleteOnCancel = deleteOnCancel;
	}
	
	public Map<ButtonOp, Style> getEmotes() {
		return emotes;
	}
	
	protected void finishEmotes() {
		emotes = Collections.unmodifiableMap(emotes);
	}
}
