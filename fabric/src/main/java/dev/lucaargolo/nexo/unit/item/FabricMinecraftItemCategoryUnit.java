package dev.lucaargolo.nexo.unit.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FabricMinecraftItemCategoryUnit extends MinecraftItemCategoryUnit {

    public FabricMinecraftItemCategoryUnit(
            @NotNull NexoMinecraft nexo,
            @NotNull ItemCategoryBase feature,
            @Nullable Role role,
            @NotNull CreativeModeTab tab
    ) {
        super(nexo, feature, role, tab);
        ResourceKey<CreativeModeTab> tabKey = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).orElseThrow();
        ItemGroupEvents.modifyEntriesEvent(tabKey).register(this::modifyEntries);
    }

    private void modifyEntries(FabricItemGroupEntries entries) {
        for (ItemUnit<?> added : addedItems) {
            if (added instanceof MinecraftItemUnit mu) {
                entries.prepend(mu.get());
            }
        }
        entries.getDisplayStacks().removeIf(stack ->
                removedItems.stream().anyMatch(r -> {
                    if (r instanceof MinecraftItemUnit mu) {
                        return ItemStack.isSameItemSameComponents(mu.get(), stack);
                    }
                    return false;
                })
        );
    }

}
