package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.*;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class ItemBase extends Feature<ItemBase, ItemUnit<?>> implements ItemProvider, RendererProvider<ItemUnit<?>>, TickerProvider<ItemUnit<?>>, InitialDataProvider {

    public ItemBase(@NotNull Location location) {
        super(location);
    }

    public ItemBase(@NotNull Location location, @NotNull Supplier<Role> role) {
        super(location, role);
    }

    @Override
    public final @NotNull Type<ItemBase, ItemUnit<?>> type() {
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
