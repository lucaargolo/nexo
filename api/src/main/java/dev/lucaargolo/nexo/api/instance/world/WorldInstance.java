package dev.lucaargolo.nexo.api.instance.world;

import dev.lucaargolo.nexo.api.feature.world.NexoWorld;
import dev.lucaargolo.nexo.api.instance.Instance;
import dev.lucaargolo.nexo.api.instance.SideProvider;
import dev.lucaargolo.nexo.api.instance.block.BlockInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

public abstract class WorldInstance extends Instance<NexoWorld> implements SideProvider {

    protected WorldInstance(@NotNull NexoWorld feature) {
        super(NexoWorld.class, feature);
    }

    @Nullable
    public abstract BlockInstance getBlock(@NotNull Vector3i pos);

    public abstract void setBlock(@NotNull Vector3i pos, @NotNull BlockInstance block);

}
