package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.api.feature.item.IItemCategory;
import net.minecraft.core.Holder;
import net.minecraft.world.item.CreativeModeTab;

public class MinecraftItemCategory extends MinecraftFeature<CreativeModeTab, IItemCategory> implements IItemCategory {

    public MinecraftItemCategory(Holder<CreativeModeTab> holder, IItemCategory delegate) {
        super(holder, delegate);
    }

}
