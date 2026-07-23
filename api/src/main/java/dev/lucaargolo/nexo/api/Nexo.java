package dev.lucaargolo.nexo.api;

import dev.lucaargolo.nexo.api.event.Event;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

public interface Nexo {

    @NotNull Logger getLogger();

    @Nullable Mod getMod(@NotNull String id);

    byte @Nullable [] loadResource(@NotNull Location location);

    @Nullable <T extends Feature<T, U>, U extends Unit<T, ?>> T getFeature(@NotNull Feature.Type<T, U> type, @NotNull Location location);

    @NotNull <T extends Feature<T, U>, U extends Unit<T, ?>> T registerFeature(@NotNull T feature);

    @Nullable <T extends Feature<T, U>, U extends Unit<T, ?>> U unit(@NotNull Feature<T, U> feature);

    @Nullable <T extends Resource<T>> T getResource(@NotNull Resource.Type<T> type, @NotNull Location location);

    @NotNull <T extends Resource<T>> T registerResource(@NotNull T resource);

    <E extends Event<T>, T> void on(@NotNull Class<E> eventType, @NotNull Event.Priority priority, @NotNull Predicate<E> listener);

    default <E extends Event<T>, T> void on(@NotNull Class<E> eventType, @NotNull Predicate<E> listener) {
        on(eventType, Event.Priority.NORMAL, listener);
    }

    <E extends Event<T>, T> void off(@NotNull Class<E> eventType, @NotNull Predicate<E> listener);

    @Nullable <E extends Event<T>, T> T emit(@NotNull E event);

    @SuppressWarnings("unchecked")
    static <T> @NotNull Class<T> type(@NotNull Class<?> type) {
        return (Class<T>) type;
    }

    record Mod(
            @NotNull String value,
            @NotNull String name,
            @NotNull String description,
            @NotNull String version,
            @NotNull List<String> authors,
            @NotNull Path path
    ) {}

}
