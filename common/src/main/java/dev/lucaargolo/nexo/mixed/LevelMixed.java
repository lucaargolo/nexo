package dev.lucaargolo.nexo.mixed;

import dev.lucaargolo.nexo.unit.world.MinecraftWorldUnit;
import org.jetbrains.annotations.Nullable;

public interface LevelMixed {

    @Nullable MinecraftWorldUnit<?> nexo$getUnit();

    void nexo$setUnit(MinecraftWorldUnit<?> unit);
}
