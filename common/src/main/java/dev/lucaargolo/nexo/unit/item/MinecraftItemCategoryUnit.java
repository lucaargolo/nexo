package dev.lucaargolo.nexo.unit.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemCategoryUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public abstract class MinecraftItemCategoryUnit<N extends NexoMinecraft> extends ItemCategoryUnit implements MinecraftUnit<CreativeModeTab> {

    protected final @NotNull N nexo;
    protected final @NotNull CreativeModeTab tab;

    protected final Set<ItemUnit> addedItems = ConcurrentHashMap.newKeySet();
    protected final Set<ItemUnit> removedItems = ConcurrentHashMap.newKeySet();

    public MinecraftItemCategoryUnit(
            @NotNull N nexo,
            @NotNull ItemCategoryBase feature,
            @Nullable Role role,
            @NotNull CreativeModeTab tab
    ) {
        super(nexo, feature, role);
        this.nexo = nexo;
        this.tab = tab;
    }

    @Override
    public @NotNull CreativeModeTab get() {
        return tab;
    }

    @Override
    public @NotNull Stream<ItemUnit> stream() {
        return tab.getDisplayItems().stream().map(nexo::stackToUnit).filter(Objects::nonNull);
    }

    @Override
    public void add(@NotNull ItemUnit item) {
        addedItems.add(item);
    }

    @Override
    public void remove(@NotNull ItemUnit item) {
        removedItems.add(item);
    }

    @Override
    public @NotNull <U extends Unit<?>> Set<String> vaults(@NotNull Class<U> type) {
        return Set.of();
    }

    @Override
    public @Nullable <U extends Unit<?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        return null;
    }



}
