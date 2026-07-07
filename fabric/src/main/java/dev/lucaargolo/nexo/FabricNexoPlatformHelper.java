package dev.lucaargolo.nexo;

import com.mojang.datafixers.util.Either;
import dev.lucaargolo.nexo.api.feature.item.NexoItemCategory;
import dev.lucaargolo.nexo.api.util.Location;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class FabricNexoPlatformHelper extends NexoPlatformHelper<FabricNexoMinecraft> {

    private final Map<ResourceKey<?>, Supplier<?>> deferredRegistries = new HashMap<>();

    @Nullable
    private static MinecraftServer currentServer;

    public FabricNexoPlatformHelper(FabricNexoMinecraft nexo) {
        super(nexo);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> currentServer = null);
        DynamicRegistrySetupCallback.EVENT.register(view -> {
            deferredRegistries.forEach((key, feature) -> {
                view.getOptional(key.registryKey()).ifPresent(registry -> {
                    Registry.registerForHolder((Registry) registry, key.location(), feature.get());
                });
            });
        });
    }

    public <T> Holder<T> registerFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature) {
        return Registry.registerForHolder(registry, id, feature.get());
    }

    @Override
    public <T> Holder<T> registerFeature(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation id, Supplier<T> feature) {
        ResourceKey<T> key = ResourceKey.create(registryKey, id);
        deferredRegistries.put(key, feature);
        return new DeferredHolder<>(key);
    }

    public Supplier<CreativeModeTab> createCreativeTab(NexoItemCategory category) {
        Location location = category.location();
        Component title = Component.translatable("itemGroup."+location.namespace()+"."+location.path());
        return () -> FabricItemGroup.builder().title(title).build();
    }

    public RegistryAccess getRegistryAccess() {
        if (currentServer != null) {
            if(currentServer.isSameThread()) {
                return currentServer.registryAccess();
            }else{
                return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            }
        }
        return RegistryAccess.EMPTY;
    }

    private class DeferredHolder<T> implements Holder<T> {

        private final ResourceKey<T> key;
        private Holder<T> delegate;

        private DeferredHolder(ResourceKey<T> key) {
            this.key = key;
        }

        @Override
        public @NotNull T value() {
            return delegateOrThrow().value();
        }

        @Override
        public boolean isBound() {
            return delegate() != null && delegate.isBound();
        }

        @Override
        public boolean is(ResourceLocation pLocation) {
            return key.location().equals(pLocation);
        }

        @Override
        public boolean is(ResourceKey<T> pResourceKey) {
            return key == pResourceKey;
        }

        @Override
        public boolean is(TagKey<T> pTagKey) {
            return delegate() != null && delegate.is(pTagKey);
        }

        @Override
        public boolean is(Holder<T> pHolder) {
            return pHolder.is(key);
        }

        @Override
        public boolean is(Predicate<ResourceKey<T>> pPredicate) {
            return pPredicate.test(key);
        }

        @Override
        public @NotNull Stream<TagKey<T>> tags() {
            return delegate() != null ? delegate.tags() : Stream.empty();
        }

        @Override
        public @NotNull Either<ResourceKey<T>, T> unwrap() {
            return delegate() != null ? delegate.unwrap() : Either.left(key);
        }

        @Override
        public @NotNull Optional<ResourceKey<T>> unwrapKey() {
            return Optional.of(key);
        }

        @Override
        public @NotNull Kind kind() {
            return Kind.REFERENCE;
        }

        @Override
        public boolean canSerializeIn(HolderOwner<T> owner) {
            return delegate() != null && delegate.canSerializeIn(owner);
        }

        @Nullable
        private Holder<T> delegate() {
            if (delegate == null) {
                RegistryAccess access = getRegistryAccess();
                delegate = access.registry(key.registryKey()).flatMap(r -> r.getHolder(key)).orElse(null);
            }
            return delegate;
        }

        private Holder<T> delegateOrThrow() {
            return Optional.ofNullable(delegate()).orElseThrow();
        }
    }

}
