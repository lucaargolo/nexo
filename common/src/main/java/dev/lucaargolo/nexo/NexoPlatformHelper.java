package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.feature.item.IItemCategory;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Supplier;

public abstract class NexoPlatformHelper {

    public abstract <T, F extends T> Holder.Reference<F> registerFeature(Registry<T> registry, ResourceLocation id, Supplier<F> feature);

    public abstract CreativeModeTab createCreativeTab(IItemCategory category);
}
