package dev.lucaargolo.nexo.api.unit.world;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.SideProvider;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

public abstract class WorldUnit<C extends Role> extends Unit<C> implements SideProvider {

    protected WorldUnit(@NotNull Nexo nexo, @NotNull WorldBase feature, @Nullable C role) {
        super(nexo, feature, role);
    }

    public abstract @Nullable BlockUnit<?> getBlock(@NotNull Vector3i pos);

    public abstract void setBlock(@NotNull Vector3i pos, @NotNull BlockUnit<?> block);

}
