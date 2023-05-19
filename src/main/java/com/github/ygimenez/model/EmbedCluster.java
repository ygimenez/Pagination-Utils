package com.github.ygimenez.model;

import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Class used for adding more than one embed per {@link Page}.
 */
public class EmbedCluster {
    private final List<MessageEmbed> embeds;

    /**
     * Creates a new {@link MessageEmbed} cluster to be used for multi-embed pages. While this constructor allows null
     * entries, it's advised not to as it'll likely result in errors during pagination.
     *
     * @param embeds The embeds to be used, amount must be between 1 and 10.
     */
    public EmbedCluster(@NotNull List<MessageEmbed> embeds) {
        if (embeds.size() < 1 || embeds.size() >= 10) {
            throw new IllegalArgumentException("Embed count must be at least 1 and at most 10");
        }

        this.embeds = embeds;
    }

    /**
     * Creates a new {@link MessageEmbed} cluster to be used for multi-embed pages. This constructor will use an
     * immutable {@link List}, and cannot receive null values.
     *
     * @param embeds The embeds to be used, amount must be between 1 and 10.
     * @throws NullPointerException If any of the embeds is null.
     */
    public EmbedCluster(@NotNull MessageEmbed... embeds) {
        this(List.of(embeds));
    }

    /**
     * Returns the internal list of embeds contained within this cluster.
     *
     * @return The stored list of {@link MessageEmbed}s.
     */
    public List<MessageEmbed> getEmbeds() {
        return embeds;
    }
}
