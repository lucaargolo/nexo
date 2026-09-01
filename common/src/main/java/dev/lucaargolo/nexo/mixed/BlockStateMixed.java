package dev.lucaargolo.nexo.mixed;

import dev.lucaargolo.nexo.unit.block.MinecraftBlockUnit;
import org.jetbrains.annotations.Nullable;

public interface BlockStateMixed {

    @Nullable MinecraftBlockUnit<?, ?> nexo$getUnit();

    void nexo$setUnit(MinecraftBlockUnit<?, ?> unit);
}
