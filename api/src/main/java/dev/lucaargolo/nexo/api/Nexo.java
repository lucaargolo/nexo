package dev.lucaargolo.nexo.api;

import dev.lucaargolo.nexo.api.event.IEvent;
import dev.lucaargolo.nexo.api.feature.IFeature;
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

    @NotNull Map<Location, IFeature> getFeatureRegistry(@NotNull IFeature.Type type);

    @Nullable IFeature getFeature(@NotNull IFeature.Type type, @NotNull Location location);

    @Nullable IFeature registerFeature(@NotNull IFeature feature);

    <E extends IEvent<T>, T> void on(@NotNull Class<E> eventType, @NotNull IEvent.Priority priority, @NotNull Predicate<E> listener);

    default <E extends IEvent<T>, T> void on(@NotNull Class<E> eventType, @NotNull Predicate<E> listener) {
        on(eventType, IEvent.Priority.NORMAL, listener);
    }

    <E extends IEvent<T>, T> void off(@NotNull Class<E> eventType, @NotNull Predicate<E> listener);

    @Nullable <E extends IEvent<T>, T> T emit(@NotNull E event);

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
