package com.github.ygimenez.model;

import com.github.ygimenez.method.Pages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Class used for checking whether a library action (read: paginate, categorize, buttonize or lazy-paginate action)
 * was already disposed of.
 * <br>
 * This is a weak reference, so its value might become null at any moment.
 * @see java.lang.ref.WeakReference
 */
public class ActionReference extends WeakReference<String> {

	/**
	 * Creates a new {@link ActionReference} for tracking a specific event key. Not for external usage.
	 *
	 * @param referent The key referring to an existing library event.
	 */
	public ActionReference(@NotNull String referent) {
		super(referent);
	}

	/**
	 * Retrieves the referred action key if it is still active, or null otherwise.
	 *
	 * @return The key used to represent the library action in the event map.
	 */
	@Nullable
	@Override
	public String get() {
		if (!Pages.getHandler().checkEvent(super.get())) {
			enqueue();
		}

		return super.get();
	}

	/**
	 * Utility method to check whether the referred action is still active.
	 * <br>
	 * Same as doing {@code get() != null}.
	 *
	 * @return Whether the action is still active or not.
	 */
	public boolean check() {
		return get() != null;
	}
}
