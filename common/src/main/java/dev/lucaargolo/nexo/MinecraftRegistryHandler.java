package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
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

public abstract class MinecraftRegistryHandler<N extends NexoMinecraft> {

    private final Map<ResourceKey<?>, Registry<?>> customRegistries = new LinkedHashMap<>();

    protected final Map<ResourceKey<?>, Consumer<Registry<?>>> dynamicRegistrars = new LinkedHashMap<>();
    protected final Map<ResourceKey<?>, Holder<?>> dynamicHolders = new LinkedHashMap<>();

    private final N nexo;

    public MinecraftRegistryHandler(N nexo) {
        this.nexo = nexo;
    }

    public N nexo() {
        return nexo;
    }

    public void init() {
        MinecraftFeatureType.all().forEach(type -> {
            if (type.customRegistry()) {
                this.getOrCreateRegistry(type.registry());
            }
            this.addBuiltinRegistryListener(type);
        });
    }

    public void beginFeatureRegistration() {
    }

    public void endFeatureRegistration() {
    }

    public abstract <T> Holder<T> registerBuiltinFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature);

    public final <T> Holder<T> registerBuiltinFeature(ResourceKey<Registry<T>> registryKey, ResourceLocation id, Supplier<T> feature) {
        return registerBuiltinFeature(getOrCreateRegistry(registryKey), id, feature);
    }

    protected final <T> Registry<T> getOrCreateRegistry(ResourceKey<Registry<T>> registryKey) {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        return registries.registry(registryKey).orElseGet(() -> {
            Registry<?> existing = customRegistries.get(registryKey);
            if (existing == null) {
                existing = createRegistry(registryKey);
                customRegistries.put(registryKey, existing);
            }
            Class<Registry<T>> clazz = Nexo.type(Registry.class);
            return clazz.cast(existing);
        });
    }

    protected abstract <T> Registry<T> createRegistry(ResourceKey<Registry<T>> registryKey);

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

    protected abstract <M> void addBuiltinRegistryListener(MinecraftFeatureType<?, ?, M> type);

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
