package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.Identifier;

import java.util.List;

import org.jetbrains.annotations.NotNull;

public interface Feature {

    @NotNull Identifier id();
    @NotNull List<@NotNull Tag> tags();

    default boolean is(@NotNull Tag tag) {
        return tags().contains(tag);
    }

}
