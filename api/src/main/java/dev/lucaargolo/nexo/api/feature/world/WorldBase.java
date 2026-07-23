package dev.lucaargolo.nexo.api.feature.world;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class WorldBase extends Feature<WorldBase, WorldUnit<?>> {

    public WorldBase(@NotNull Location location) {
        super(location);
    }

    public WorldBase(@NotNull Location location, @NotNull Supplier<Role> role) {
        super(location, role);
    }

    @Override
    public final @NotNull Type<WorldBase, WorldUnit<?>> type() {
        return Type.WORLD;
    }
}
