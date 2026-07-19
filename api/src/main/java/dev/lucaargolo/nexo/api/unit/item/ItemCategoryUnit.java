package dev.lucaargolo.nexo.api.unit.item;

import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public abstract class ItemCategoryUnit<C extends Role> extends Unit<C> {

    protected ItemCategoryUnit(@NotNull ItemCategoryBase feature, @Nullable C role) {
        super(feature, role);
    }

    @Override
    public @NotNull <R extends Role> ItemCategoryUnit<R> with(@NotNull Class<R> type) {
        return (ItemCategoryUnit<R>) super.with(type);
    }

    public abstract @NotNull Stream<ItemUnit<?>> stream();

    public abstract void add(@NotNull ItemUnit<?> item);

    public abstract void remove(@NotNull ItemUnit<?> item);

}
