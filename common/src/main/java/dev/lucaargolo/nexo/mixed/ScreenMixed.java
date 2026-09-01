package dev.lucaargolo.nexo.mixed;

import dev.lucaargolo.nexo.unit.screen.MinecraftScreenUnit;
import org.jetbrains.annotations.Nullable;

public interface ScreenMixed {

    @Nullable MinecraftScreenUnit<?> nexo$getUnit();

    void nexo$setUnit(MinecraftScreenUnit<?> unit);
}
