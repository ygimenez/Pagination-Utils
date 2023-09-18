package com.github.ygimenez.model;

/**
 * Utility class holding library-wide settings.
 */
public abstract class PUtilsConfig {
	private static LogLevel logLevel = LogLevel.LEVEL_1;

	private PUtilsConfig() {
	}

	/**
	 * Levels used to filter what events are logged to the console by the library.
	 */
	public enum LogLevel {
		/**
		 * Disables all event logging.
		 */
		NONE,
		/**
		 * Logs only important events such as errors and dangerous actions (equivalent to ERROR level).
		 */
		LEVEL_1,
		/**
		 * All previous events plus minor issues (equivalent to WARN level).
		 */
		LEVEL_2,
		/**
		 * All previous events plus hints and general information (equivalent to INFO level).
		 */
		LEVEL_3,
		/**
		 * All previous events plus debugging messages (equivalent to DEBUG level).
		 */
		LEVEL_4
	}

	/**
	 * Retrieves the {@link LogLevel} currently configured.
	 *
	 * @return The current {@link LogLevel}.
	 */
	public static LogLevel getLogLevel() {
		return logLevel;
	}

	/**
	 * Set the library's {@link LogLevel}.
	 *
	 * @param logLevel The desired {@link LogLevel} (default: {@link LogLevel#LEVEL_1}).
	 */
	public static void setLogLevel(LogLevel logLevel) {
		PUtilsConfig.logLevel = logLevel;
	}
}
