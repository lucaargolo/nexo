package dev.lucaargolo.nexo.api.unit.block;

import dev.lucaargolo.nexo.api.feature.block.NexoBlock;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;

public abstract class BlockUnit extends Unit<NexoBlock> {

    protected BlockUnit(@NotNull NexoBlock feature) {
        super(NexoBlock.class, feature);
    }

}
