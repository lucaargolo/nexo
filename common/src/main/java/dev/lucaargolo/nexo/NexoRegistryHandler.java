package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.util.NexoHolder;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class NexoRegistryHandler<N extends NexoMinecraft> {

    protected static final Map<ResourceKey<?>, Consumer<Registry<?>>> dynamicRegistrars = new LinkedHashMap<>();
    protected static final Map<ResourceKey<?>, NexoHolder<?, ?>> dynamicHolders = new LinkedHashMap<>();

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

    @SuppressWarnings("unchecked")
    public <R, T extends R> NexoHolder<R, T> registerDynamicFeature(ResourceKey<? extends Registry<R>> registryKey, ResourceLocation id, Supplier<T> feature, Class<T> type) {
        ResourceKey<R> key = ResourceKey.create(registryKey, id);
        Consumer<Registry<?>> registrar = registry -> Registry.registerForHolder((Registry<T>) registry, key.location(), feature.get());
        dynamicRegistrars.put(key, registrar);
        NexoHolder<R, T> holder = new NexoHolder<>(this.nexo(), key, type);
        dynamicHolders.put(key, holder);
        return holder;
    }

    @SuppressWarnings("unchecked")
    public <R> NexoHolder<R, ? extends R> getDynamicFeature(ResourceKey<R> key) {
        return (NexoHolder<R, ? extends R>) dynamicHolders.get(key);
    }

    public abstract <D> void registerDataAttachment(DataBase<D> data);

    public abstract CreativeModeTab createCreativeTab(ItemCategoryBase category);

    public RegistryAccess getRegistry() {
        if (capturedRegistry != null && Thread.currentThread() == capturedRegistryThread) {
            return capturedRegistry;
        }
        MinecraftServer currentServer = this.nexo.getServer();
        if (currentServer != null) {
            if (currentServer.isSameThread()) {
                return currentServer.registryAccess();
            } else {
                return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            }
        }
        return RegistryAccess.EMPTY;
    }

    public static void captureRegistry(RegistryAccess registry) {
        if (registry == null) {
            capturedRegistryThread = null;
            capturedRegistry = null;
        } else {
            capturedRegistryThread = Thread.currentThread();
            capturedRegistry = registry;
        }
    }

    public static Map<ResourceKey<?>, Consumer<Registry<?>>> getDynamicRegistrars() {
        return dynamicRegistrars;
    }

}
