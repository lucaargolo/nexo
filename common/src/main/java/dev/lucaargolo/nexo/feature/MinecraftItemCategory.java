package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.item.IItemCategory;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class MinecraftItemCategory extends MinecraftFeature<CreativeModeTab, IItemCategory> implements IItemCategory {

    public MinecraftItemCategory(Holder<CreativeModeTab> holder, IItemCategory delegate) {
        super(holder, delegate);
    }

    public MinecraftItemCategory(Holder<CreativeModeTab> holder) {
        super(holder, null);
    }

    public static MinecraftItemCategory register(ResourceLocation id, IItemCategory category) {
        Holder<CreativeModeTab> holder = NexoMinecraft.getHelper().registerFeature(BuiltInRegistries.CREATIVE_MODE_TAB, id, NexoMinecraft.getHelper().createCreativeTab(category));
        return new MinecraftItemCategory(holder, category);
    }
}
