package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.feature.item.NexoItemCategory;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.util.LazyHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class NeoForgeNexoPlatformHelper extends NexoPlatformHelper<NeoForgeNexoMinecraft> {

    private final Map<ResourceKey<? extends Registry<?>>, Map<String, DeferredRegister<?>>> deferredRegistries = new HashMap<>();

    public NeoForgeNexoPlatformHelper(NeoForgeNexoMinecraft nexo) {
        super(nexo);
    }

    public <T> Holder<T> registerFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature) {
        return registerFeature(registry.key(), id, feature).get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> LazyHolder<T> registerFeature(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation id, Supplier<T> feature) {
        DeferredRegister<T> deferredRegistry = (DeferredRegister<T>) deferredRegistries
                .computeIfAbsent(registryKey, r -> new HashMap<>())
                .computeIfAbsent(id.getNamespace(), n -> {
                    DeferredRegister<?> r = DeferredRegister.create(registryKey, id.getNamespace());
                    r.register(this.nexo().modBus());
                    return r;
                });
        return new LazyHolder<>(deferredRegistry.register(id.getPath(), feature));
    }

    public Supplier<CreativeModeTab> createCreativeTab(NexoItemCategory category) {
        Location location = category.location();
        Component title = Component.translatable("itemGroup."+location.namespace()+"."+location.path());
        return () -> CreativeModeTab.builder().title(title).build();
    }

    @Override
    public MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

}
