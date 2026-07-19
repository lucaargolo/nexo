package dev.lucaargolo.nexo.unit.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.item.ItemCategoryUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public abstract class MinecraftItemCategoryUnit extends ItemCategoryUnit<Role> implements MinecraftUnit<CreativeModeTab> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final CreativeModeTab tab;

    protected final Set<ItemUnit<?>> addedItems = ConcurrentHashMap.newKeySet();
    protected final Set<ItemUnit<?>> removedItems = ConcurrentHashMap.newKeySet();

    public MinecraftItemCategoryUnit(
            @NotNull NexoMinecraft nexo,
            @NotNull ItemCategoryBase feature,
            @Nullable Role role,
            @NotNull CreativeModeTab tab
    ) {
        super(feature, role);
        this.nexo = nexo;
        this.tab = tab;
    }

    @Override
    public @NotNull NexoMinecraft nexo() {
        return nexo;
    }

    @Override
    public @NotNull CreativeModeTab get() {
        return tab;
    }

    @Override
    public <D> @Nullable D getData(@NotNull DataBase<D> data) {
        return null;
    }

    @Override
    public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {
    }

    @Override
    public @NotNull Stream<ItemUnit<?>> stream() {
        return tab.getDisplayItems().stream().map(nexo::stackToUnit);
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
