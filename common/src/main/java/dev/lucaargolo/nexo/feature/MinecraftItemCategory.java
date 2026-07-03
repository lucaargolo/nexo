package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.item.IItemCategory;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

public class MinecraftItemCategory extends MinecraftFeature<CreativeModeTab, IItemCategory> implements IItemCategory {

    public MinecraftItemCategory(Holder<CreativeModeTab> holder, IItemCategory delegate) {
        super(holder, delegate);
    }

    public static MinecraftItemCategory register(NexoMinecraft nexo, ResourceLocation id, IItemCategory category) {
        return null;
    }
}
