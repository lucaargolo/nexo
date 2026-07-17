package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.ItemProvider;
import dev.lucaargolo.nexo.api.feature.RendererProvider;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class ItemBase extends Feature<ItemBase> implements RendererProvider<ItemUnit<?>>, ItemProvider {

    public ItemBase(@NotNull Location location) {
        super(location);
    }

    public ItemBase(@NotNull Location location, @NotNull Supplier<Role> role) {
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
