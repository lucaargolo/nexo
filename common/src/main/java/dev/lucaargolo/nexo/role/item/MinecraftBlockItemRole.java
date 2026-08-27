package dev.lucaargolo.nexo.role.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.item.BlockItemRole;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Utils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class MinecraftBlockItemRole {

    public static MinecraftRoleType.Info<Item, Item.Properties> craft(NexoMinecraft<?, ?, ?, ?> nexo, ItemBase base) {
        if(base.role() instanceof BlockItemRole(BlockBase block)) {
            Utils.Extender<Item> extender = Utils.extend(nexo, BlockItem.class);
            Function<Item.Properties, Item> function = properties -> extender.instantiate(MinecraftFeatureType.BLOCK.convert(block), properties);
            return new MinecraftRoleType.Info<>(extender, function);
        }
        return null;
    }

    public static BlockItemRole uncraft(NexoMinecraft<?, ?, ?, ?> nexo, Item item) {
        if(item instanceof BlockItem blockItem) {
            return new BlockItemRole(MinecraftFeatureType.BLOCK.convert(nexo, blockItem.getBlock()));
        }
        return null;
    }

}
