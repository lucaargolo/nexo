package dev.lucaargolo.nexo;

import com.mojang.serialization.Codec;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.util.LazyHolder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class FabricNexoRegistryHandler extends NexoRegistryHandler<FabricNexoMinecraft> {

    @SuppressWarnings("UnstableApiUsage")
    private static final Map<DataBase<?>, AttachmentType<?>> dataAttachmentMap = new LinkedHashMap<>();

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
        dynamicFeatures.put(key, feature);
        return new LazyHolder<>(this.nexo(), key);
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public <D> void registerDataAttachment(DataBase<D> data) {
        Location location = data.location();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
        AttachmentType<D> type = AttachmentRegistry.create(id, builder -> {
            if (data.persistent()) {
                Codec<D> codec = NexoMinecraft.createCodec(data);
                builder.persistent(codec);
                builder.copyOnDeath();
            }
            if (data.synced()) {
                StreamCodec<RegistryFriendlyByteBuf, D> codec = NexoMinecraft.createPacketCodec(data);
                builder.syncWith(codec, AttachmentSyncPredicate.all());
            }
        });
        dataAttachmentMap.put(data, type);
    }

    public Supplier<CreativeModeTab> createCreativeTab(ItemCategoryBase category) {
        Location location = category.location();
        Component title = Component.translatable("itemGroup."+location.namespace()+"."+location.path());
        return () -> FabricItemGroup.builder().title(title).build();
    }

    @SuppressWarnings({"unchecked", "UnstableApiUsage"})
    @NotNull
    public static <D> AttachmentType<D> getDataAttachment(DataBase<D> data) {
        return (AttachmentType<D>) dataAttachmentMap.get(data);
    }

}
