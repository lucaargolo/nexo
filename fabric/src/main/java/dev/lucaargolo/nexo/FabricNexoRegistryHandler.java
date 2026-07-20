package dev.lucaargolo.nexo;

import com.mojang.serialization.Codec;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.item.MinecraftItemCategory;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.api.event.registry.DynamicRegistryView;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class FabricNexoRegistryHandler extends NexoRegistryHandler<FabricNexoMinecraft> {

    private static final Map<DataBase<?>, Object> dataAttachmentMap = new LinkedHashMap<>();

    public FabricNexoRegistryHandler(FabricNexoMinecraft nexo) {
        super(nexo);
        DynamicRegistrySetupCallback.EVENT.register(view -> {
            MinecraftFeatureType.all().forEach(type -> this.addDynamicRegistryListener(view, type));
            dynamicRegistrars.forEach((key, registrar) -> {
                view.getOptional(key.registryKey()).ifPresent(registrar);
            });
        });
        MinecraftFeatureType.all().forEach(this::addBuiltinRegistryListener);
    }

    @Override
    public <T> void registerBuiltinFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature) {
        Registry.register(registry, id, feature.get());
    }

    @Override
    public <D> void registerDataAttachment(DataBase<D> data) {
        ResourceLocation id = NexoMinecraft.rl(data.location());
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

    public CreativeModeTab craftCreativeTab(ItemCategoryBase category) {
        Location location = category.location();
        Component title = Component.translatable("itemGroup."+location.namespace()+"."+location.path());
        return FabricItemGroup.builder().title(title).displayItems((parameters, output) -> {
            MinecraftItemCategory.ITEM_MAP.getOrDefault(category, List.of()).forEach(item -> {
               output.accept(MinecraftFeatureType.ITEM.convert(item));
            });
        }).build();
    }

    @Override
    protected RegistryAccess getLocalRegistry() {
        return null;
    }

    private <M> void addBuiltinRegistryListener(MinecraftFeatureType<?, M> type) {
        RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registry(type.registry()).ifPresent(registry -> {
            RegistryEntryAddedCallback.allEntries(registry, holder -> {
                this.nexo().emit(new FeatureRegisteredEvent(NexoMinecraft.id(holder), type.index(this, holder)));
            });
        });
    }

    private <M> void addDynamicRegistryListener(DynamicRegistryView view, MinecraftFeatureType<?, M> type) {
        view.registerEntryAdded(type.registry(), (raw, id, value) -> {
            Holder.Reference<M> holder = view.getOptional(type.registry()).flatMap(registry -> registry.getHolder(raw)).orElseThrow();
            this.nexo().emit(new FeatureRegisteredEvent(NexoMinecraft.id(id), type.index(this, holder)));
            dynamicHolders.put(holder.key(), holder);
        });
    }

    @SuppressWarnings("unchecked")
    public static <D> @NotNull AttachmentType<D> getDataAttachment(@NotNull DataBase<D> data) {
        return (AttachmentType<D>) dataAttachmentMap.get(data);
    }

}
