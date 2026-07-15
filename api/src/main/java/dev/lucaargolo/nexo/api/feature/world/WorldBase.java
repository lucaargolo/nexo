package dev.lucaargolo.nexo.api.feature.world;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class WorldBase extends Feature<WorldBase> {

    public WorldBase(@NotNull Location location) {
        super(location);
    }

    public WorldBase(@NotNull Location location, @Nullable Role role) {
        super(location, role);
    }

    @Override
    public final @NotNull Type<WorldBase> type() {
        return Type.WORLD;
    }
}
