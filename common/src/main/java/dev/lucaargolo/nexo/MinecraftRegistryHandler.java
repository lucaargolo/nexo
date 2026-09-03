package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.VaultFactory;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
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
        for (MinecraftFeatureType<?, ?, ?> type : MinecraftFeatureType.all()) {
            if (type.registryType() == MinecraftFeatureType.RegistryType.CUSTOM) {
                this.getOrCreateRegistry(type.registry());
            }
            if (type.registryType() != MinecraftFeatureType.RegistryType.DIRECT) {
                this.addBuiltinRegistryListener(type);
            }
        }
    }

    public void beginFeatureRegistration() {
    }

    public void endFeatureRegistration() {
    }

    public abstract <T extends Feature<T, U> & VaultFactory<U>, U extends Unit<T>, M> void registerVaults(
            @NotNull MinecraftFeatureType<T, U, M> type,
            @NotNull T feature,
            @NotNull Supplier<M> minecraft
    );

    protected final <U extends Unit<?>, V extends Unit<?>> @NotNull Map<String, Function<U, ? extends @Nullable Vault<V>>> vaultFactories(
            @NotNull VaultFactory<U> feature,
            @NotNull Class<V> type
    ) {
        Map<String, Function<U, ? extends @Nullable Vault<V>>> factories = new LinkedHashMap<>(feature.vaults(type));
        factories.forEach((key, factory) -> {
            Objects.requireNonNull(key, "Vault key");
            Objects.requireNonNull(factory, "Vault factory");
        });
        return Collections.unmodifiableMap(factories);
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
        Holder<?> holder = dynamicHolders.get(key);
        if (holder == null) {
            throw new IllegalStateException("Dynamic feature " + key.location() + " is not registered");
        }
        Class<Holder<T>> clazz = Nexo.type(Holder.class);
        return clazz.cast(holder);
    }

    public abstract <D> void registerDataAttachment(DataBase<D> data);

    public abstract CreativeModeTab craftCreativeTab(ItemCategoryBase category);

    public abstract <T extends AbstractContainerMenu, D> MenuType<T> craftMenuType(MinecraftScreen.MenuCrafter<D> crafter, DataBase<D> data);

    protected abstract RegistryAccess getLocalRegistry();

    protected abstract <M> void addBuiltinRegistryListener(MinecraftFeatureType<?, ?, M> type);

    public final RegistryAccess getRegistry() {
        RegistryAccess localRegistry = getLocalRegistry();
        if (localRegistry != null) {
            return localRegistry;
        }
        MinecraftServer currentServer = this.nexo.getServer();
        if (currentServer != null && currentServer.isSameThread()) {
            return currentServer.registryAccess();
        }
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

}
