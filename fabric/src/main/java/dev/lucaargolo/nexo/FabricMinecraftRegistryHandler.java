package dev.lucaargolo.nexo;

import com.mojang.serialization.Codec;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.VaultFactory;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.event.WorldDimensionsBakeCallback;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.item.MinecraftItemCategory;
import dev.lucaargolo.nexo.unit.FabricVaultStorage;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.LevelStem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class FabricMinecraftRegistryHandler extends MinecraftRegistryHandler<FabricNexoMinecraft> {

    public static final EntityApiLookup<Storage<ItemVariant>, Void> ENTITY_ITEM_STORAGE = EntityApiLookup.get(ResourceLocation.fromNamespaceAndPath(NexoMinecraft.MOD_ID, "entity_item_storage"), Storage.asClass(), Void.class);

    private final Map<DataBase<?>, AttachmentType<?>> dataAttachmentMap = new LinkedHashMap<>();
    private final Map<AttachmentType<?>, DataBase<?>> attachmentDataMap = new IdentityHashMap<>();
    private final List<FeatureRegisteredEvent> pendingFeatureEvents = new ArrayList<>();
    private final ThreadLocal<Set<Object>> activeVaultFeatures = ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));
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
    public <T extends Feature<T, U> & VaultFactory<U>, U extends Unit<T, ?>, M> void registerVaults(
            @NotNull MinecraftFeatureType<T, U, M> type,
            @NotNull T feature,
            @NotNull Supplier<M> minecraft
    ) {
        Class<ItemUnit<?>> itemUnitType = Nexo.type(ItemUnit.class);
        var vaultFactories = this.vaultFactories(feature, itemUnitType);
        if (vaultFactories.isEmpty()) {
            return;
        }
        M value = minecraft.get();
        if (type.minecraftType() == Block.class) {
            ItemStorage.SIDED.registerForBlocks((world, pos, state, blockEntity, direction) -> this.createVaultCapability(feature, () -> FabricVaultStorage.create(this.nexo(), this.nexo().blockToUnit(world, pos, state, blockEntity, direction), vaultFactories)), Block.class.cast(value));
        } else if (type.minecraftType() == Item.class) {
            ItemStorage.ITEM.registerForItems((stack, context) -> this.createVaultCapability(feature, () -> FabricVaultStorage.create(this.nexo(), this.nexo().stackToUnit(stack), vaultFactories)), Item.class.cast(value));
        } else if (type.minecraftType() == EntityType.class) {
            ENTITY_ITEM_STORAGE.registerForTypes((entity, context) -> this.createVaultCapability(feature, () -> FabricVaultStorage.create(this.nexo(), this.nexo().entityToUnit(entity), vaultFactories)), EntityType.class.cast(value));
        } else {
            throw new IllegalArgumentException("Unsupported vault feature type: " + type.minecraftType().getName());
        }
    }

    @Override
    public void init() {
        super.init();
        DynamicRegistrySetupCallback.EVENT.register(view -> {
            MinecraftFeatureType.all().forEach(type -> {
                Class<MinecraftFeatureType<?, ?, Object>> featureTypeClass = Nexo.type(MinecraftFeatureType.class);
                MinecraftFeatureType<?, ?, Object> typedType = featureTypeClass.cast(type);
                view.registerEntryAdded(typedType.registry(), (raw, id, value) -> {
                Holder.Reference<Object> holder = view.getOptional(typedType.registry()).flatMap(registry -> registry.getHolder(raw)).orElseThrow();
                emitFeatureRegistered(new FeatureRegisteredEvent(NexoMinecraft.id(id), typedType.index(this.nexo(), holder)));
                dynamicHolders.put(holder.key(), holder);
                });
            });
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
        attachmentDataMap.put(type, data);
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

    private void emitFeatureRegistered(FeatureRegisteredEvent event) {
        if (featureRegistrationActive) {
            pendingFeatureEvents.add(event);
        } else {
            this.nexo().emit(event);
        }
    }

    private <T> @Nullable T createVaultCapability(@NotNull Object feature, @NotNull Supplier<T> creator) {
        Set<Object> active = this.activeVaultFeatures.get();
        if (!active.add(feature)) {
            return null;
        }
        try {
            return creator.get();
        } finally {
            active.remove(feature);
            if (active.isEmpty()) {
                this.activeVaultFeatures.remove();
            }
        }
    }

    public <D> @NotNull AttachmentType<D> getDataAttachment(@NotNull DataBase<D> data) {
        Class<AttachmentType<D>> clazz = Nexo.type(AttachmentType.class);
        return clazz.cast(dataAttachmentMap.get(data));
    }

    public @Nullable DataBase<?> getAttachmentData(@NotNull AttachmentType<?> type) {
        return attachmentDataMap.get(type);
    }

}
