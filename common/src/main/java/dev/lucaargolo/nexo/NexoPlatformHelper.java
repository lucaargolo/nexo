package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.item.NexoItemCategory;
import dev.lucaargolo.nexo.util.LazyHolder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class NexoPlatformHelper<N extends Nexo> {

    private final N nexo;

    @Nullable
    protected Thread capturedRegistryThread;
    @Nullable
    protected RegistryAccess capturedRegistry;

    public NexoPlatformHelper(N nexo) {
        this.nexo = nexo;
    }

    public N nexo() {
        return nexo;
    }

    public abstract <T> Holder<T> registerBuiltinFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature);

    public abstract <T> LazyHolder<T> registerDynamicFeature(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation id, Supplier<T> feature);

    public abstract Supplier<CreativeModeTab> createCreativeTab(NexoItemCategory category);

    public abstract MinecraftServer getServer();

    public RegistryAccess getRegistry() {
        if (this.capturedRegistry != null && Thread.currentThread() == this.capturedRegistryThread) {
            return this.capturedRegistry;
        }
        MinecraftServer currentServer = this.getServer();
        if (currentServer != null) {
            if(currentServer.isSameThread()) {
                return currentServer.registryAccess();
            }else{
                return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            }
        }
        return RegistryAccess.EMPTY;
    }

    public void captureRegistry(RegistryAccess registry) {
        if(registry == null) {
            this.capturedRegistryThread = null;
            this.capturedRegistry = null;
        }else{
            this.capturedRegistryThread = Thread.currentThread();
            this.capturedRegistry = registry;
        }
    }

    public RegistryFriendlyByteBuf befriend(ByteBuf buf) {
        return new RegistryFriendlyByteBuf(buf, this.getRegistry());
    }

}
