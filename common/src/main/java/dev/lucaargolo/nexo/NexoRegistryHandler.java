package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class NexoRegistryHandler<N extends NexoMinecraft> {

    protected final Map<ResourceKey<?>, Consumer<Registry<?>>> dynamicRegistrars = new LinkedHashMap<>();
    protected final Map<ResourceKey<?>, Holder<?>> dynamicHolders = new LinkedHashMap<>();

    private final N nexo;

    public NexoRegistryHandler(N nexo) {
        this.nexo = nexo;
    }

    public N nexo() {
        return nexo;
    }

    public abstract void init();

    public abstract <T> void registerBuiltinFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature);

    public <T> void registerDynamicFeature(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation id, Supplier<T> feature) {
        ResourceKey<T> key = ResourceKey.create(registryKey, id);
        dynamicRegistrars.put(key, r -> {
            Class<Registry<T>> clazz = Nexo.type(Registry.class);
            dynamicHolders.put(key, Registry.registerForHolder(clazz.cast(r), key.location(), feature.get()));
        });
    }

    public <T> Holder<T> getDynamicFeature(ResourceKey<T> key) {
        Holder<?> holder = Objects.requireNonNull(dynamicHolders.get(key));
        Class<Holder<T>> clazz = Nexo.type(Holder.class);
        return clazz.cast(holder);
    }

    public abstract <D> void registerDataAttachment(DataBase<D> data);

    public abstract CreativeModeTab craftCreativeTab(ItemCategoryBase category);

    protected abstract RegistryAccess getLocalRegistry();

    public final RegistryAccess getRegistry() {
        RegistryAccess localRegistry = getLocalRegistry();
        if(localRegistry != null) {
            return localRegistry;
        }
        MinecraftServer currentServer = this.nexo.getServer();
        if (currentServer != null) {
            if (currentServer.isSameThread()) {
                return currentServer.registryAccess();
            }
        }
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

}
