package dev.lucaargolo.nexo.api;

import dev.lucaargolo.nexo.api.event.Event;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;

public interface Nexo {

    @NotNull Logger getLogger();

    @Nullable Mod getMod(@NotNull String id);

    byte @Nullable [] loadResource(@NotNull Location location);

    @Nullable <T extends Feature<T>> T getFeature(@NotNull Feature.Type<T> type, @NotNull Location location);

    @NotNull <T extends Feature<T>> T registerFeature(@NotNull T feature);

    <E extends Event<T>, T> void on(@NotNull Class<E> eventType, @NotNull Event.Priority priority, @NotNull Predicate<E> listener);

    default <E extends Event<T>, T> void on(@NotNull Class<E> eventType, @NotNull Predicate<E> listener) {
        on(eventType, Event.Priority.NORMAL, listener);
    }

    <E extends Event<T>, T> void off(@NotNull Class<E> eventType, @NotNull Predicate<E> listener);

    @Nullable <E extends Event<T>, T> T emit(@NotNull E event);

    //TODO: Replace with get resource, create IResource with IResource.Type. Model will be an IResource, resources can be generated on runtime or loaded from filesystem.
    @Nullable Model getModel(@NotNull Location location);

    record Mod(
            @NotNull String value,
            @NotNull String name,
            @NotNull String description,
            @NotNull String version,
            @NotNull String[] authors,
            @NotNull Path path
    ) {}

}
