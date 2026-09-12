package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.VaultFactory;
import dev.lucaargolo.nexo.api.feature.VaultProvider;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.fluid.MinecraftFluid;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class MinecraftRegistryHandler {

    private final Map<ResourceKey<?>, Registry<?>> customRegistries = new LinkedHashMap<>();

    protected final Map<ResourceKey<?>, Consumer<Registry<?>>> dynamicRegistrars = new LinkedHashMap<>();
    protected final Map<ResourceKey<?>, Holder<?>> dynamicHolders = new LinkedHashMap<>();

    private final List<FeatureRegisteredEvent> pendingFeatureEvents = new ArrayList<>();
    private boolean featureRegistrationActive = false;

    private final NexoMinecraft nexo;

    public MinecraftRegistryHandler(NexoMinecraft nexo) {
        this.nexo = nexo;
    }

    public NexoMinecraft nexo() {
        return nexo;
    }

    public void init() {
        for (MinecraftFeatureType<?, ?> type : MinecraftFeatureType.all()) {
            if (type.registryType() == MinecraftFeatureType.RegistryType.CUSTOM) {
                this.getOrCreateRegistry(type.registry());
            }
            if (type.registryType() != MinecraftFeatureType.RegistryType.DIRECT) {
                this.addBuiltinRegistryListener(type);
            }
        }
    }

    public void beginFeatureRegistration() {
        featureRegistrationActive = true;
    }

    public void endFeatureRegistration() {
        featureRegistrationActive = false;
        List<FeatureRegisteredEvent> events = List.copyOf(pendingFeatureEvents);
        pendingFeatureEvents.clear();
        events.forEach(this.nexo()::emit);
    }

    protected void emitFeatureRegistered(FeatureRegisteredEvent event) {
        if (featureRegistrationActive) {
            pendingFeatureEvents.add(event);
        } else {
            this.nexo().emit(event);
        }
    }

    public abstract <T> Holder<T> registerBuiltinFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature);

    public final <T> Holder<T> registerBuiltinFeature(ResourceKey<Registry<T>> registryKey, ResourceLocation id, Supplier<T> feature) {
        return registerBuiltinFeature(getOrCreateRegistry(registryKey), id, feature);
    }

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

    protected abstract <M> void addBuiltinRegistryListener(MinecraftFeatureType<?, M> type);

    public abstract <D> void registerDataAttachment(DataBase<D> data);

    public abstract void registerFluidType(@NotNull MinecraftFluid.Entry entry);

    public abstract CreativeModeTab craftCreativeTab(ItemCategoryBase category);

    public abstract <D> MinecraftScreen.ExtendedMenuType<D> craftMenuType(@NotNull ScreenBase<D> screen, @NotNull MinecraftScreen.MenuCrafter<D> menuCrafter, @NotNull MinecraftScreen.ScreenCrafter<D> screenCrafter);

    public abstract @NotNull MinecraftFluid.ExtendedFluid craftFluid(@NotNull MinecraftFluid.Entry entry, boolean source);

    public abstract <T extends Feature<T, U> & VaultFactory<U>, U extends Unit<T> & VaultProvider, M> void registerVaults(@NotNull MinecraftFeatureType<T, M> type, @NotNull T feature, @NotNull Supplier<M> minecraft);

    protected final <U extends Unit<?>> @NotNull List<Vault.Slotted<U>> createVaults(@NotNull Unit<?> unit, @NotNull Map<String, ? extends Function<?, ? extends Vault.Slotted<U>>> vaultFactories) {
        List<String> names = new ArrayList<>(vaultFactories.keySet());
        names.sort(String::compareTo);
        List<Vault.Slotted<U>> vaults = new ArrayList<>(names.size());
        Class<Function<Unit<?>, ? extends Vault.Slotted<U>>> type = Nexo.type(Function.class);
        for (String name : names) {
            Function<?, ? extends Vault.Slotted<U>> factory = Objects.requireNonNull(vaultFactories.get(name), "Vault factory '" + name + "' is null");
            Vault.Slotted<U> vault = Objects.requireNonNull(type.cast(factory).apply(unit), "Vault factory '" + name + "' returned null");
            vaults.add(vault);
        }
        return List.copyOf(vaults);
    }

    protected abstract RegistryAccess localAccess();

    public final RegistryAccess access() {
        RegistryAccess localAccess = localAccess();
        if (localAccess != null) {
            return localAccess;
        }
        MinecraftServer currentServer = this.nexo.getServer();
        if (currentServer != null && currentServer.isSameThread()) {
            return currentServer.registryAccess();
        }
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    public final Feature.Identity<MinecraftRegistryHandler> identity(@NotNull Holder<?> holder) {
        return new Identity(NexoMinecraft.id(holder));
    }

    public final Feature.Identity<MinecraftRegistryHandler> identity(@NotNull Location location) {
        return new Identity(location);
    }

    public class Identity implements Feature.Identity<MinecraftRegistryHandler> {

        private final Location location;

        public Identity(Location location) {
            this.location = location;
        }

        @Override
        public @NotNull Nexo nexo() {
            return MinecraftRegistryHandler.this.nexo;
        }

        @Override
        public @NotNull MinecraftRegistryHandler authority() {
            return MinecraftRegistryHandler.this;
        }

        @Override
        public @NotNull Location location() {
            return this.location;
        }

    }

}
