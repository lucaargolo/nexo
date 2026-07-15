package dev.lucaargolo.nexo.role.item;

import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.item.BlockItemRole;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.Optional;

public class MinecraftBlockItemRole {

    public static Optional<BlockItem> craft(ItemBase base) {
        if(base.role() instanceof BlockItemRole(BlockBase block)) {
            BlockItem item = new BlockItem(MinecraftFeatureType.BLOCK.convert(block), new Item.Properties());
            return Optional.of(item);
        }
        return Optional.empty();
    }

    public static Optional<BlockItemRole> uncraft(NexoRegistryHandler<?> helper, Item item) {
        if(item instanceof BlockItem blockItem) {
            return Optional.of(new BlockItemRole(MinecraftFeatureType.BLOCK.convert(helper, blockItem.getBlock())));
        }
        return Optional.empty();
    }

}
