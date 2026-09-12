package dev.lucaargolo.nexo.api.feature.fluid;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.fluid.FluidUnit;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class FluidBase extends Feature<FluidBase, FluidUnit> {

    public FluidBase() {

    }

    public FluidBase(@NotNull Supplier<Role> role) {
        super(role);
    }

    @Override
    public final @NotNull Type<FluidBase, FluidUnit> type() {
        return Type.FLUID;
    }

}
