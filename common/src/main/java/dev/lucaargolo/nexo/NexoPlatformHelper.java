package dev.lucaargolo.nexo;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public abstract class NexoPlatformHelper {

    public abstract <T, F extends T> Holder.Reference<F> registerFeature(Registry<T> registry, ResourceLocation id, Supplier<F> feature);

}
