package dev.lucaargolo.nexo.unit.item;

import dev.lucaargolo.nexo.NeoForgeMinecraftRegistryHandler;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NeoForgeMinecraftItemCategoryUnit<C extends Role> extends MinecraftItemCategoryUnit<NeoForgeMinecraftRegistryHandler, C> {

    public NeoForgeMinecraftItemCategoryUnit(
            @NotNull NeoForgeMinecraftRegistryHandler helper,
            @NotNull ItemCategoryBase feature,
            @Nullable C role,
            @NotNull CreativeModeTab tab
    ) {
        super(helper, feature, role, tab);
        helper.nexo().modBus().addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTab() == tab) {
                for (ItemUnit<?> added : addedItems) {
                    if (added instanceof MinecraftItemUnit<?> mu) {
                        event.insertFirst(mu.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    }
                }
                tab.getDisplayItems().removeIf(stack ->
                    removedItems.stream().anyMatch(r -> {
                        if (r instanceof MinecraftItemUnit<?> mu) {
                            return ItemStack.isSameItemSameComponents(mu.get(), stack);
                        }
                        return false;
                    })
                );
            }
        });
    }

}
