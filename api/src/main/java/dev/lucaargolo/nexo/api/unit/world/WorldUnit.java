package dev.lucaargolo.nexo.api.unit.world;

import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.SideProvider;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

public abstract class WorldUnit<C extends Role> extends Unit<C> implements SideProvider {

    protected WorldUnit(@NotNull WorldBase feature, @Nullable C role) {
        super(feature, role);
    }

    @Override
    public @NotNull <R extends Role> WorldUnit<R> with(@NotNull Class<R> type) {
        return (WorldUnit<R>) super.with(type);
    }

    public abstract @Nullable BlockUnit<?> getBlock(@NotNull Vector3i pos);

    public abstract void setBlock(@NotNull Vector3i pos, @NotNull BlockUnit<?> block);

}
