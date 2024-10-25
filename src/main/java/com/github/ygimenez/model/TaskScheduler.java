package com.github.ygimenez.model;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Task manager for pagination events' expiration
 */
public class TaskScheduler {
	private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
	private final Map<String, ScheduledFuture<?>> tasks = new HashMap<>();

	/**
	 * Schedule a new task, cancelling and replacing the previous if exists.
	 *
	 * @param id   The task identifier
	 * @param task The task itself
	 * @param time The time for the timeout
	 * @param unit The unit for the timeout
	 * @return The task that will be executed
	 */
	public ScheduledFuture<?> schedule(String id, Runnable task, long time, TimeUnit unit) {
		ScheduledFuture<?> t = worker.schedule(task, time, unit);
		ScheduledFuture<?> prev = tasks.put(id, t);
		if (prev != null) {
			prev.cancel(true);
		}

		return t;
	}
}
