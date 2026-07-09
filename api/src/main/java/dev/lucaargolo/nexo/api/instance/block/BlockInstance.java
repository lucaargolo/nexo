package dev.lucaargolo.nexo.api.instance.block;

import dev.lucaargolo.nexo.api.feature.block.NexoBlock;
import dev.lucaargolo.nexo.api.instance.Instance;
import org.jetbrains.annotations.NotNull;

public abstract class BlockInstance extends Instance<NexoBlock> {

    protected BlockInstance(@NotNull NexoBlock feature) {
        super(NexoBlock.class, feature);
    }

}
