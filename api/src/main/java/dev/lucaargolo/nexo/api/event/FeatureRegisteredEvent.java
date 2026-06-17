package dev.lucaargolo.nexo.api.event;

import dev.lucaargolo.nexo.api.Location;
import dev.lucaargolo.nexo.api.feature.IFeature;

public record FeatureRegisteredEvent<T extends IFeature>(Location location, T feature) implements IEvent<T> {

    @Override
    public T value() {
        return this.feature;
    }

    @Override
    public boolean cancelable() {
        return false;
    }

}
