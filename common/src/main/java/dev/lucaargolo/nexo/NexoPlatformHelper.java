package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.feature.item.IItemCategory;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Supplier;

public abstract class NexoPlatformHelper {

    public abstract <T, F extends T> Holder.Reference<F> registerFeature(Registry<T> registry, ResourceLocation id, Supplier<F> feature);

    public abstract CreativeModeTab createCreativeTab(IItemCategory category);

    public abstract RegistryAccess getRegistryAccess();

    public RegistryFriendlyByteBuf befriend(ByteBuf buf) {
        return new RegistryFriendlyByteBuf(buf, this.getRegistryAccess());
    }

}
