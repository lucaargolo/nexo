package dev.lucaargolo.nexo.api.feature.world;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

public abstract class WorldBase extends Feature<WorldBase> {

    public WorldBase(@NotNull Location location) {
        super(location);
    }

    @Override
    public final @NotNull Type<WorldBase> type() {
        return Type.WORLD;
    }
}
