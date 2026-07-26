package dev.lucaargolo.nexo.role.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.item.BlockItemRole;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.NexoUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class MinecraftBlockItemRole {

    public static MinecraftRoleType.Info<Item, Item.Properties> craft(NexoMinecraft nexo, ItemBase base) {
        if(base.role() instanceof BlockItemRole(BlockBase block)) {
            NexoUtils.Extender<Item> extender = NexoUtils.extend(nexo, BlockItem.class);
            Function<Item.Properties, Item> function = properties -> extender.instantiate(MinecraftFeatureType.BLOCK.convert(block), properties);
            return new MinecraftRoleType.Info<>(extender, function);
        }
        return null;
    }

    public static BlockItemRole uncraft(NexoRegistryHandler<?> helper, Item item) {
        if(item instanceof BlockItem blockItem) {
            return new BlockItemRole(MinecraftFeatureType.BLOCK.convert(helper, blockItem.getBlock()));
        }
        return null;
    }

}
