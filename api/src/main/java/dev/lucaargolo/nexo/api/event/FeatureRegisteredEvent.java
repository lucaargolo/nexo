package dev.lucaargolo.nexo.api.event;

import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.util.Location;

public record FeatureRegisteredEvent<T extends IFeature>(Location location, T value) implements IEvent<T> {

    @Override
    public boolean cancelable() {
        return false;
    }

}
