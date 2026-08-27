package dev.lucaargolo.nexo;

import com.mojang.serialization.Codec;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.event.WorldDimensionsBakeCallback;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.item.MinecraftItemCategory;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.api.event.registry.DynamicRegistryView;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.dimension.LevelStem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class FabricMinecraftRegistryHandler extends MinecraftRegistryHandler<FabricNexoMinecraft> {

    private final Map<DataBase<?>, AttachmentType<?>> dataAttachmentMap = new LinkedHashMap<>();
    private final List<FeatureRegisteredEvent> pendingFeatureEvents = new ArrayList<>();
    private boolean featureRegistrationActive;

    public FabricMinecraftRegistryHandler(FabricNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public void beginFeatureRegistration() {
        featureRegistrationActive = true;
    }

    @Override
    public void endFeatureRegistration() {
        featureRegistrationActive = false;
        List<FeatureRegisteredEvent> events = List.copyOf(pendingFeatureEvents);
        pendingFeatureEvents.clear();
        events.forEach(this.nexo()::emit);
    }

    @Override
    public void init() {
        super.init();
        DynamicRegistrySetupCallback.EVENT.register(view -> {
            MinecraftFeatureType.all().forEach(type -> this.addDynamicRegistryListener(view, type));
            dynamicRegistrars.forEach((key, registrar) -> {
                view.getOptional(key.registryKey()).ifPresent(registrar);
            });
        });
        WorldDimensionsBakeCallback.EVENT.register((registry, dimensions) -> {
            dimensions.forEach((key, stem) -> {
                Holder<LevelStem> holder = registry.getHolderOrThrow(key);
                MinecraftFeatureType.WORLD.index(this.nexo(), holder);
            });
        });
    }

    @Override
    public <T> Holder<T> registerBuiltinFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature) {
        return Registry.registerForHolder(registry, id, feature.get());
    }

    @Override
    protected <T> Registry<T> createRegistry(ResourceKey<Registry<T>> registryKey) {
        return FabricRegistryBuilder.createSimple(registryKey).buildAndRegister();
    }

    @Override
    public <D> void registerDataAttachment(DataBase<D> data) {
        ResourceLocation id = NexoMinecraft.rl(data.location());
        AttachmentType<D> type = AttachmentRegistry.create(id, builder -> {
            builder.initializer(data::initial);
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
        Component title = Component.translatable(category.languageKey());
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

    @Override
    protected <M> void addBuiltinRegistryListener(MinecraftFeatureType<?, ?, M> type) {
        RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registry(type.registry()).ifPresent(registry -> {
            RegistryEntryAddedCallback.allEntries(registry, holder -> {
                emitFeatureRegistered(new FeatureRegisteredEvent(NexoMinecraft.id(holder), type.index(this.nexo(), holder)));
            });
        });
    }

    private <M> void addDynamicRegistryListener(DynamicRegistryView view, MinecraftFeatureType<?, ?, M> type) {
        view.registerEntryAdded(type.registry(), (raw, id, value) -> {
            Holder.Reference<M> holder = view.getOptional(type.registry()).flatMap(registry -> registry.getHolder(raw)).orElseThrow();
            emitFeatureRegistered(new FeatureRegisteredEvent(NexoMinecraft.id(id), type.index(this.nexo(), holder)));
            dynamicHolders.put(holder.key(), holder);
        });
    }

    private void emitFeatureRegistered(FeatureRegisteredEvent event) {
        if (featureRegistrationActive) {
            pendingFeatureEvents.add(event);
        } else {
            this.nexo().emit(event);
        }
    }


    public <D> @NotNull AttachmentType<D> getDataAttachment(@NotNull DataBase<D> data) {
        Class<AttachmentType<D>> clazz = Nexo.type(AttachmentType.class);
        return clazz.cast(dataAttachmentMap.get(data));
    }

}
