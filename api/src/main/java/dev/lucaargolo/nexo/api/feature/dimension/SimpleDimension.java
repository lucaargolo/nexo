package dev.lucaargolo.nexo.api.feature.dimension;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

public class SimpleDimension extends NexoDimension {

    @NotNull
    private final Location location;

    public SimpleDimension(@NotNull Location location) {
        this.location = location;
    }

    @Override
    public @NotNull Location location() {
        return location;
    }

}
