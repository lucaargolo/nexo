package dev.lucaargolo.nexo.api;

import dev.lucaargolo.nexo.api.event.IEvent;
import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.function.Predicate;

public interface Nexo {

    @NotNull Logger getLogger();

    @Nullable NexoMod getMod(String id);

    byte @Nullable [] loadResource(@NotNull Location location);

    @Nullable <T extends IFeature> T getFeature(Class<T> type, Location location);

    @Nullable <T extends IFeature, I extends T> T registerFeature(Class<T> type, I feature);

    @NotNull <T extends IFeature> Map<Location, IFeature> getFeatureRegistry(Class<T> type);


    <E extends IEvent<T>, T> void on(@NotNull Class<E> eventType, @NotNull IEvent.Priority priority, @NotNull Predicate<E> listener);

    default <E extends IEvent<T>, T> void on(@NotNull Class<E> eventType, @NotNull Predicate<E> listener) {
        on(eventType, IEvent.Priority.NORMAL, listener);
    }

    <E extends IEvent<T>, T> void off(@NotNull Class<E> eventType, @NotNull Predicate<E> listener);

    @Nullable <E extends IEvent<T>, T> T emit(@NotNull E event);

    @Nullable Model getModel(Location location);

}
