package dev.lucaargolo.nexo.api;

import dev.lucaargolo.nexo.api.event.IEvent;
import dev.lucaargolo.nexo.api.feature.IFeature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface Nexo {

    @Nullable <T extends IFeature> T getFeature(Class<T> type, Location location);

    @Nullable <T extends IFeature, I extends T> T registerFeature(Class<T> type, Location location, I feature);

    <E extends IEvent<T>, T> void on(@NotNull Class<E> eventType, @NotNull Predicate<E> listener);

    <E extends IEvent<T>, T> void off(@NotNull Class<E> eventType, @NotNull Predicate<E> listener);

    <E extends IEvent<T>, T> @Nullable T emit(@NotNull E event);

}
