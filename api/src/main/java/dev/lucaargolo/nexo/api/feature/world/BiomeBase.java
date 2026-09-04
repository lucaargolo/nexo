package dev.lucaargolo.nexo.api.feature.world;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class BiomeBase extends Feature<BiomeBase, Unit<BiomeBase>> {

    public BiomeBase() {

    }

    public BiomeBase(@NotNull Supplier<Role> role) {
        super(role);
    }

    @Override
    public final @NotNull Type<BiomeBase, Unit<BiomeBase>> type() {
        return Type.BIOME;
    }
}
