package dev.lucaargolo.nexo.api.feature.fluid;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class FluidBase extends Feature<FluidBase, Unit<FluidBase>> {

    public FluidBase() {

    }

    public FluidBase(@NotNull Supplier<Role> role) {
        super(role);
    }

    @Override
    public final @NotNull Type<FluidBase, Unit<FluidBase>> type() {
        return Type.FLUID;
    }

}
