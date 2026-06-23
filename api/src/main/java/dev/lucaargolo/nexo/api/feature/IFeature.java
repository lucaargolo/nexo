package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IFeature {

    @NotNull Location location();
    @NotNull List<@NotNull Tag> tags();

    default boolean is(@NotNull Tag tag) {
        return tags().contains(tag);
    }

    record Tag(Location location) {}

}
