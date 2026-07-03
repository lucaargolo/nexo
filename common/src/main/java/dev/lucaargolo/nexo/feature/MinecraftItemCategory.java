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

    public static MinecraftItemCategory register(NexoMinecraft nexo, ResourceLocation id, IItemCategory category) {
        Holder.Reference<CreativeModeTab> holder = nexo.getHelper().registerFeature(BuiltInRegistries.CREATIVE_MODE_TAB, id, () -> {
            return nexo.getHelper().createCreativeTab(category);
        });
        return new MinecraftItemCategory(holder, category);
    }
}
