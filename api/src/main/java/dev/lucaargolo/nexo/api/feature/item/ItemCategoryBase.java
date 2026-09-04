package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.item.ItemCategoryUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class ItemCategoryBase extends Feature<ItemCategoryBase, ItemCategoryUnit> {

    public ItemCategoryBase() {

    }

    public ItemCategoryBase(@NotNull Supplier<Role> role) {
        super(role);
    }

    @Override
    public final @NotNull Type<ItemCategoryBase, ItemCategoryUnit> type() {
        return Type.ITEM_CATEGORY;
    }

}
