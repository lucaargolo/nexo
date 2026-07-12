package dev.lucaargolo.nexo.api.unit.block;

import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;

public abstract class BlockUnit extends Unit<BlockBase> {

    protected BlockUnit(@NotNull BlockBase feature) {
        super(BlockBase.class, feature);
    }

}
