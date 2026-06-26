package dev.lucaargolo.nexo.api.event;

import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.util.Location;

import org.jetbrains.annotations.NotNull;
public record FeatureRegisteredEvent<T extends IFeature>(@NotNull Location location, T value) implements IEvent<T> {

    @Override
    public boolean cancelable() {
        return false;
    }

}
