package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.item.NexoItemCategory;
import dev.lucaargolo.nexo.util.LazyHolder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Supplier;

public abstract class NexoPlatformHelper<N extends Nexo> {

    private final N nexo;

    public NexoPlatformHelper(N nexo) {
        this.nexo = nexo;
    }

    public N nexo() {
        return nexo;
    }

    public abstract <T> Holder<T> registerFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature);

    public abstract <T> LazyHolder<T> registerFeature(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation id, Supplier<T> feature);

    public abstract Supplier<CreativeModeTab> createCreativeTab(NexoItemCategory category);

    public abstract RegistryAccess getRegistryAccess();

    public abstract void captureRegistry(RegistryAccess registry);

    public RegistryFriendlyByteBuf befriend(ByteBuf buf) {
        return new RegistryFriendlyByteBuf(buf, this.getRegistryAccess());
    }

}
