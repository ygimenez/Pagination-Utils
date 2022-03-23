package com.github.ygimenez.model;

import com.github.ygimenez.method.Pages;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class ActionReference extends WeakReference<String> {
	public ActionReference(String referent) {
		super(referent);
	}

	public ActionReference(String referent, ReferenceQueue<? super String> q) {
		super(referent, q);
	}

	@Nullable
	@Override
	public String get() {
		if (!Pages.getHandler().checkEvent(super.get()))
			enqueue();

		return super.get();
	}

	public boolean check() {
		return get() != null;
	}
}
