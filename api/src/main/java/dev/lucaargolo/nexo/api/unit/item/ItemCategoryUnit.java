package dev.lucaargolo.nexo.api.unit.item;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public abstract class ItemCategoryUnit extends Unit<ItemCategoryBase> {

    protected ItemCategoryUnit(@NotNull Nexo nexo, @NotNull ItemCategoryBase feature, @Nullable Role role) {
        super(nexo, feature, role);
    }

    public abstract @NotNull Stream<ItemUnit> stream();

    public abstract void add(@NotNull ItemUnit item);

    public abstract void remove(@NotNull ItemUnit item);

}
