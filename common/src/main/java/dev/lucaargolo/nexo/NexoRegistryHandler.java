package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.util.NexoHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class NexoRegistryHandler<N extends NexoMinecraft> {

    protected static final Map<ResourceKey<?>, Supplier<?>> dynamicFeatures = new LinkedHashMap<>();

    @Nullable
    protected static Thread capturedRegistryThread;
    @Nullable
    protected static RegistryAccess capturedRegistry;

    private final N nexo;

    public NexoRegistryHandler(N nexo) {
        this.nexo = nexo;
    }

    public N nexo() {
        return nexo;
    }

    public abstract <R, T extends R> NexoHolder<R, T> registerBuiltinFeature(Registry<R> registry, ResourceLocation id, Supplier<T> feature);

    public abstract <R, T extends R> NexoHolder<R, T> registerDynamicFeature(ResourceKey<? extends Registry<R>> registryKey, ResourceLocation id, Supplier<T> feature, Class<T> type);

    public abstract <D> void registerDataAttachment(DataBase<D> data);

    public abstract Supplier<CreativeModeTab> createCreativeTab(ItemCategoryBase category);

    public RegistryAccess getRegistry() {
        if (capturedRegistry != null && Thread.currentThread() == capturedRegistryThread) {
            return capturedRegistry;
        }
        MinecraftServer currentServer = this.nexo.getServer();
        if (currentServer != null) {
            if(currentServer.isSameThread()) {
                return currentServer.registryAccess();
            }else{
                return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            }
        }
        return RegistryAccess.EMPTY;
    }

    public static void captureRegistry(RegistryAccess registry) {
        if(registry == null) {
            capturedRegistryThread = null;
            capturedRegistry = null;
        }else{
            capturedRegistryThread = Thread.currentThread();
            capturedRegistry = registry;
        }
    }

    public static Map<ResourceKey<?>, Supplier<?>> getDynamicFeatures() {
        return dynamicFeatures;
    }

}
