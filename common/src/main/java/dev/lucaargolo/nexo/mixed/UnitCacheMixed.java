package dev.lucaargolo.nexo.mixed;

import dev.lucaargolo.nexo.unit.MinecraftUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface UnitCacheMixed {

    @Nullable MinecraftUnit<?> nexo$getUnit();

    void nexo$setUnit(@NotNull MinecraftUnit<?> unit);
}
