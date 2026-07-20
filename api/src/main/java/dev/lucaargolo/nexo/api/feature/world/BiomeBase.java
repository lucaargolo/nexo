package dev.lucaargolo.nexo.api.feature.world;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class BiomeBase extends Feature<BiomeBase> {

    public BiomeBase(@NotNull Location location) {
        super(location);
    }

    public BiomeBase(@NotNull Location location, @NotNull Supplier<Role> role) {
        super(location, role);
    }

    @Override
    public final @NotNull Type<BiomeBase> type() {
        return Type.BIOME;
    }
}
