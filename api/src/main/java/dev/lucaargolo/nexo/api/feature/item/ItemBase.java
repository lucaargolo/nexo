package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.ItemProvider;
import dev.lucaargolo.nexo.api.feature.ModelProvider;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ItemBase extends Feature<ItemBase> implements ModelProvider, ItemProvider {

    public ItemBase(@NotNull Location location) {
        super(location);
    }

    public ItemBase(@NotNull Location location, @Nullable Role role) {
        super(location, role);
    }

    @Override
    public final @NotNull Type<ItemBase> type() {
        return Type.ITEM;
    }

    @Override
    public @NotNull ItemBase item() {
        return this;
    }

    public @Nullable ItemCategoryBase category() {
        return null;
    }

}
