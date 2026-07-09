package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.feature.item.NexoItemCategory;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.util.LazyHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class NeoForgeNexoRegistryHandler extends NexoRegistryHandler<NeoForgeNexoMinecraft> {

    private final Map<Registry<?>, Map<String, DeferredRegister<?>>> deferredRegistries = new HashMap<>();

    public NeoForgeNexoRegistryHandler(NeoForgeNexoMinecraft nexo) {
        super(nexo);
    }

    @SuppressWarnings("unchecked")
    public <T> Holder<T> registerBuiltinFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature) {
        DeferredRegister<T> deferredRegistry = (DeferredRegister<T>) deferredRegistries
                .computeIfAbsent(registry, r -> new HashMap<>())
                .computeIfAbsent(id.getNamespace(), n -> {
                    DeferredRegister<?> r = DeferredRegister.create(registry, id.getNamespace());
                    r.register(this.nexo().modBus());
                    return r;
                });
        return deferredRegistry.register(id.getPath(), feature);
    }

    @Override
    public <T> LazyHolder<T> registerDynamicFeature(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation id, Supplier<T> feature) {
        ResourceKey<T> key = ResourceKey.create(registryKey, id);
        dynamicFeatures.put(key, feature);
        return new LazyHolder<>(this.nexo(), key);
    }

    public Supplier<CreativeModeTab> createCreativeTab(NexoItemCategory category) {
        Location location = category.location();
        Component title = Component.translatable("itemGroup."+location.namespace()+"."+location.path());
        return () -> CreativeModeTab.builder().title(title).build();
    }

}
