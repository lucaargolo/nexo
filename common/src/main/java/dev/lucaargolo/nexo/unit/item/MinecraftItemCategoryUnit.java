package dev.lucaargolo.nexo.unit.item;

import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.item.ItemCategoryUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public abstract class MinecraftItemCategoryUnit<R extends NexoRegistryHandler<?>, C extends Role> extends ItemCategoryUnit<Role> implements MinecraftUnit<CreativeModeTab> {

    protected final @NotNull R helper;
    protected final @NotNull CreativeModeTab tab;

    protected final Set<ItemUnit<?>> addedItems = ConcurrentHashMap.newKeySet();
    protected final Set<ItemUnit<?>> removedItems = ConcurrentHashMap.newKeySet();

    public MinecraftItemCategoryUnit(
            @NotNull R helper,
            @NotNull ItemCategoryBase feature,
            @Nullable C role,
            @NotNull CreativeModeTab tab
    ) {
        super(helper.nexo(), feature, role);
        this.helper = helper;
        this.tab = tab;
    }

    @Override
    public @NotNull CreativeModeTab get() {
        return tab;
    }

    @Override
    public @NotNull Stream<ItemUnit<?>> stream() {
        return tab.getDisplayItems().stream().map(helper.nexo()::stackToUnit);
    }

    @Override
    public void add(@NotNull ItemUnit<?> item) {
        addedItems.add(item);
    }

    @Override
    public void remove(@NotNull ItemUnit<?> item) {
        removedItems.add(item);
    }



}
