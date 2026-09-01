package dev.lucaargolo.nexo.mixed;

import dev.lucaargolo.nexo.unit.item.MinecraftItemCategoryUnit;
import org.jetbrains.annotations.Nullable;

public interface CreativeModeTabMixed {

    @Nullable MinecraftItemCategoryUnit<?, ?> nexo$getUnit();

    void nexo$setUnit(MinecraftItemCategoryUnit<?, ?> unit);
}
