package dev.lucaargolo.nexo.unit.item;

import dev.lucaargolo.nexo.NeoForgeNexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NeoForgeMinecraftItemCategoryUnit<C extends Role> extends MinecraftItemCategoryUnit<NeoForgeNexoRegistryHandler, C> {

    public NeoForgeMinecraftItemCategoryUnit(
            @NotNull NeoForgeNexoRegistryHandler helper,
            @NotNull ItemCategoryBase feature,
            @Nullable C role,
            @NotNull CreativeModeTab tab
    ) {
        super(helper, feature, role, tab);
        helper.nexo().modBus().addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTab() == tab) {
                for (ItemUnit<?> added : addedItems) {
                    if (added instanceof MinecraftItemUnit<?> mu) {
                        event.accept(mu.get());
                    }
                }
                tab.getDisplayItems().removeIf(stack ->
                    removedItems.stream().anyMatch(r -> {
                        if (r instanceof MinecraftItemUnit<?> mu) {
                            return mu.get().getItem() == stack.getItem();
                        }
                        return false;
                    })
                );
            }
        });
    }

}
