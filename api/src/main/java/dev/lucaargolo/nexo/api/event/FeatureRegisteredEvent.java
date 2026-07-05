package dev.lucaargolo.nexo.api.event;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.util.Location;

import org.jetbrains.annotations.NotNull;
public record FeatureRegisteredEvent(@NotNull Location location, @NotNull Feature<?> value) implements Event<Feature<?>> {

    @Override
    public boolean cancelable() {
        return false;
    }

}
