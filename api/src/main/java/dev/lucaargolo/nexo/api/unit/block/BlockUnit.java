package dev.lucaargolo.nexo.api.unit.block;

import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;

public abstract class BlockUnit<T extends BlockBase> extends Unit<T> {

    protected BlockUnit(@NotNull T feature) {
        super(feature);
    }

}
