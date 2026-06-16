package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.Location;

import java.util.List;

import org.jetbrains.annotations.NotNull;

public interface IFeature {

    @NotNull Location location();
    @NotNull List<@NotNull Tag> tags();

    default boolean is(@NotNull Tag tag) {
        return tags().contains(tag);
    }

    record Tag(Location location) {}

}
