package dev.lucaargolo.nexo.api.feature.data;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

public final class StringData extends DataBase.StringData {

    @NotNull
    private final String initial;
    @NotNull
    private final Location location;

    public StringData(@NotNull String initial, @NotNull Location location) {
        this.initial = initial;
        this.location = location;
    }

    @Override
    public String initial() {
        return this.initial;
    }

    @Override
    @NotNull
    public Location location() {
        return this.location;
    }

}
