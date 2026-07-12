package dev.lucaargolo.nexo.api.feature.data;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

public final class StringData extends DataBase.StringData {

    @NotNull
    private final String initial;

    public StringData(@NotNull Location location, @NotNull String initial) {
        super(location);
        this.initial = initial;
    }

    @Override
    @NotNull
    public String initial() {
        return this.initial;
    }

}
