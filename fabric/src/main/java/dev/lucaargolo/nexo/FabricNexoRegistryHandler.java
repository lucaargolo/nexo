package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.feature.item.NexoItemCategory;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.util.LazyHolder;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Supplier;

public class FabricNexoRegistryHandler extends NexoRegistryHandler<FabricNexoMinecraft> {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public FabricNexoRegistryHandler(FabricNexoMinecraft nexo) {
        super(nexo);
        DynamicRegistrySetupCallback.EVENT.register(view -> {
            dynamicFeatures.forEach((key, feature) -> {
                view.getOptional(key.registryKey()).ifPresent(registry -> {
                    Registry.registerForHolder((Registry) registry, key.location(), feature.get());
                });
            });
        });
    }

    public <T> Holder<T> registerBuiltinFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature) {
        return Registry.registerForHolder(registry, id, feature.get());
    }

    @Override
    public <T> LazyHolder<T> registerDynamicFeature(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation id, Supplier<T> feature) {
        ResourceKey<T> key = ResourceKey.create(registryKey, id);
        this.dynamicFeatures.put(key, feature);
        return new LazyHolder<>(this.nexo(), key);
    }

    public Supplier<CreativeModeTab> createCreativeTab(NexoItemCategory category) {
        Location location = category.location();
        Component title = Component.translatable("itemGroup."+location.namespace()+"."+location.path());
        return () -> FabricItemGroup.builder().title(title).build();
    }

}
