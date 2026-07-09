package dev.lucaargolo.nexo.api.feature.world;

import dev.lucaargolo.nexo.api.feature.Instance;
import dev.lucaargolo.nexo.api.feature.block.NexoBlock;
import dev.lucaargolo.nexo.api.feature.dimension.NexoDimension;
import dev.lucaargolo.nexo.api.util.Side;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

public abstract class WorldInstance extends Instance<NexoDimension> {

    protected WorldInstance(@Nullable NexoDimension feature, @NotNull Side side) {
        super(feature, side);
    }

    @Nullable
    public abstract Instance<NexoBlock> getBlock(@NotNull Vector3i pos);

    public abstract void setBlock(@NotNull Vector3i pos, @NotNull Instance<NexoBlock> block);

}
