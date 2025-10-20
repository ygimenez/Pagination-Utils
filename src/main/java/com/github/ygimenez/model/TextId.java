package com.github.ygimenez.model;

import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Subclass of {@link ButtonId} to represent text-only buttons.
 */
public class TextId implements ButtonId<String> {
	public static final Pattern ID_PATTERN = Pattern.compile("\\.(?=\\d+$)");

	private final String id;
	private final String label;
	private final ButtonStyle style;

	/**
	 * Creates a new instance. The label will be the same as the ID.
	 * @param id The {@link String} to be used, cannot be empty.
	 */
	public TextId(@NotNull String id) {
		this(id, id, ButtonStyle.SECONDARY);
	}

	/**
	 * Creates a new instance with a chosen style.
	 * @param id The {@link String} to be used, cannot be empty.
	 * @param label The label to be used.
	 */
	public TextId(@NotNull String id, String label) {
		this(id, label, ButtonStyle.SECONDARY);
	}

	/**
	 * Creates a new instance with a chosen style.
	 * @param id The {@link String} to be used, cannot be empty.
	 * @param style The {@link ButtonStyle} to be used.
	 */
	public TextId(@NotNull String id, @NotNull ButtonStyle style) {
		this(id, id, style);
	}

	/**
	 * Creates a new instance with a chosen style.
	 * @param id The {@link String} to be used, cannot be empty.
	 * @param label The label to be used, must not be null.
	 * @param style The {@link ButtonStyle} to be used.
	 */
	public TextId(@NotNull String id, @NotNull String label, @NotNull ButtonStyle style) {
		if (id.contains(".")) {
			id = ID_PATTERN.split(id)[0];
		}

		this.id = id;
		this.label = label;
		this.style = style;
	}

	@Override
	public @NotNull String getId() {
		return id;
	}

	@NotNull
	@Override
	public String getLabel() {
		return label;
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
