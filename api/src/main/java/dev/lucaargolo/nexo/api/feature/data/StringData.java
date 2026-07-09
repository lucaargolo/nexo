package dev.lucaargolo.nexo.api.feature.data;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

public final class StringData extends NexoData.StringData {

    @NotNull
    private final Location location;

    public StringData(@NotNull Location location) {
        this.location = location;
    }

    @Override
    @NotNull
    public Location location() {
        return location;
    }

}
