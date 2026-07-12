package dev.lucaargolo.nexo.api.unit.world;

import dev.lucaargolo.nexo.api.feature.world.NexoWorld;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.SideProvider;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

public abstract class WorldUnit extends Unit<NexoWorld> implements SideProvider {

    protected WorldUnit(@NotNull NexoWorld feature) {
        super(NexoWorld.class, feature);
    }

    @Nullable
    public abstract BlockUnit getBlock(@NotNull Vector3i pos);

    public abstract void setBlock(@NotNull Vector3i pos, @NotNull BlockUnit block);

}
