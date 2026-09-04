package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.*;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class ItemBase extends Feature<ItemBase, ItemUnit> implements ItemProvider, VaultFactory<ItemUnit>, RendererProvider<Graphics3D, ItemUnit>, TickerProvider<ItemUnit>, DataInitializer {

    public ItemBase() {

    }

    public ItemBase(@NotNull Supplier<Role> role) {
        super(role);
    }

    @Override
    public final @NotNull Type<ItemBase, ItemUnit> type() {
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
