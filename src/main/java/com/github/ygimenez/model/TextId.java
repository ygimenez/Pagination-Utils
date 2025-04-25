package com.github.ygimenez.model;

import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Subclass of {@link ButtonId} to represent text-only buttons.
 */
public class TextId implements ButtonId<String> {
	private static final Pattern ID_PATTERN = Pattern.compile("\\.(?=[^.]+$)");
	private final String id;
	private final ButtonStyle style;

	/**
	 * Creates a new instance.
	 * @param id The {@link String} to be used
	 */
	public TextId(@NotNull String id) {
		this(id, ButtonStyle.SECONDARY);
	}

	/**
	 * Creates a new instance with chosen style.
	 * @param id The {@link String} to be used
	 * @param style The {@link ButtonStyle} to be used.
	 */
	public TextId(@NotNull String id, @NotNull ButtonStyle style) {
		if (id.contains(".")) {
			id = ID_PATTERN.split(id)[0];
		}

		this.id = id;
		this.style = style;
	}

	@Override
	public @NotNull String getId() {
		return id;
	}

	@Override
	public ButtonStyle getStyle() {
		return style;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ButtonId<?>)) return false;
		ButtonId<?> textId = (ButtonId<?>) o;
		return Objects.equals(id, textId.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
