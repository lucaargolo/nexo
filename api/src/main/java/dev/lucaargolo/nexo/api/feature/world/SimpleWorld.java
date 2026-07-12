package dev.lucaargolo.nexo.api.feature.world;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

public class SimpleWorld extends WorldBase {

    @NotNull
    private final Location location;

    public SimpleWorld(@NotNull Location location) {
        this.location = location;
    }

    @Override
    public @NotNull Location location() {
        return location;
    }

}
