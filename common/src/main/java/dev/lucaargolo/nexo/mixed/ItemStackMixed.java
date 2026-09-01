package dev.lucaargolo.nexo.mixed;

import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import org.jetbrains.annotations.Nullable;

public interface ItemStackMixed {

    @Nullable MinecraftItemUnit<?> nexo$getUnit();

    void nexo$setUnit(MinecraftItemUnit<?> unit);
}
