package dev.lucaargolo.nexo.api.feature.world;

import dev.lucaargolo.nexo.api.feature.DataInitializer;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.TickerProvider;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class WorldBase extends Feature<WorldBase, WorldUnit> implements TickerProvider<WorldUnit>, DataInitializer {

    public WorldBase() {

    }

    public WorldBase(@NotNull Supplier<Role> role) {
        super(role);
    }

    @Override
    public final @NotNull Type<WorldBase, WorldUnit> type() {
        return Type.WORLD;
    }
}
