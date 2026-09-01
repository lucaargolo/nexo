package dev.lucaargolo.nexo.mixed;

import dev.lucaargolo.nexo.unit.entity.MinecraftEntityUnit;
import org.jetbrains.annotations.Nullable;

public interface EntityMixed {

    @Nullable MinecraftEntityUnit<?, ?, ?> nexo$getUnit();

    void nexo$setUnit(MinecraftEntityUnit<?, ?, ?> unit);
}
