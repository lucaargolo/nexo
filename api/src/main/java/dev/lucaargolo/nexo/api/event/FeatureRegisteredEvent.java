package dev.lucaargolo.nexo.api.event;

import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.util.Location;

import org.jetbrains.annotations.NotNull;
public record FeatureRegisteredEvent(@NotNull Location location, @NotNull IFeature<?> value) implements IEvent<IFeature<?>> {

    @Override
    public boolean cancelable() {
        return false;
    }

}
