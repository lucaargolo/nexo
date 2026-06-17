package dev.lucaargolo.nexo.api.event;

import dev.lucaargolo.nexo.api.Location;
import dev.lucaargolo.nexo.api.feature.IFeature;

public record FeatureRegisteredEvent<T extends IFeature>(Location location, T value) implements IEvent<T> {

    @Override
    public boolean cancelable() {
        return false;
    }

}
