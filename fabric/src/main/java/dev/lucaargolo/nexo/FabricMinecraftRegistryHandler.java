package dev.lucaargolo.nexo;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.VaultFactory;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.event.WorldDimensionsBakeCallback;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.item.MinecraftItemCategory;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import dev.lucaargolo.nexo.unit.FabricVaultItemStorage;
import dev.lucaargolo.nexo.unit.screen.MinecraftScreenUnit;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.dimension.LevelStem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class FabricMinecraftRegistryHandler extends MinecraftRegistryHandler {

    public static final EntityApiLookup<Storage<ItemVariant>, Void> ENTITY_ITEM_STORAGE = EntityApiLookup.get(ResourceLocation.fromNamespaceAndPath(NexoMinecraft.MOD_ID, "entity_item_storage"), Storage.asClass(), Void.class);

    private final Map<DataBase<?>, AttachmentType<?>> dataAttachmentMap = new LinkedHashMap<>();
    private final Map<AttachmentType<?>, DataBase<?>> attachmentDataMap = new IdentityHashMap<>();

    private final ThreadLocal<Set<Object>> activeVaultFeatures = ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

    public FabricMinecraftRegistryHandler(FabricNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public void init() {
        super.init();
        DynamicRegistrySetupCallback.EVENT.register(view -> {
            MinecraftFeatureType.all().forEach(type -> {
                if (type.registryType() == MinecraftFeatureType.RegistryType.DIRECT) {
                    return;
                }
                Class<MinecraftFeatureType<?, Object>> featureTypeClass = Nexo.type(MinecraftFeatureType.class);
                MinecraftFeatureType<?, Object> typedType = featureTypeClass.cast(type);
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
    protected RegistryAccess getLocalRegistry() {
        return null;
    }

    @Override
    public <T> Holder<T> registerBuiltinFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature) {
        return Registry.registerForHolder(registry, id, feature.get());
    }

    @Override
    public CreativeModeTab craftCreativeTab(ItemCategoryBase category) {
        Component title = Component.translatable(category.languageKey());
        return FabricItemGroup.builder().title(title).displayItems((parameters, output) -> {
            MinecraftItemCategory.ITEM_MAP.getOrDefault(category, List.of()).forEach(item -> {
                output.accept(MinecraftFeatureType.ITEM.convert(item));
            });
        }).build();
    }

    @Override
    public <D> MinecraftScreen.ExtendedMenuType<D> craftMenuType(@NotNull ScreenBase<D> screen, @NotNull MinecraftScreen.MenuCrafter<D> menuCrafter, @NotNull MinecraftScreen.ScreenCrafter<D> screenCrafter) {
        return new ExtendedMenuTypeImpl<>(screen, menuCrafter, screenCrafter);
    }

    private final class ExtendedMenuTypeImpl<D> extends ExtendedScreenHandlerType<AbstractContainerMenu, Pair<D, byte[]>> implements MinecraftScreen.ExtendedMenuType<D> {

        private final MinecraftScreen.MenuCrafter<D> menuCrafter;
        private final MinecraftScreen.ScreenCrafter<D> screenCrafter;

        public ExtendedMenuTypeImpl(@NotNull ScreenBase<D> screen, MinecraftScreen.MenuCrafter<D> menuCrafter, MinecraftScreen.ScreenCrafter<D> screenCrafter) {
            super((id, inventory, pair) -> {
                Level level = inventory.player.level();
                RegistryFriendlyByteBuf ownerBuffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(pair.getSecond()), level.registryAccess());
                Unit<?> owner;
                try {
                    owner = MinecraftScreenUnit.decodeOwner(nexo(), ownerBuffer, level);
                } finally {
                    ownerBuffer.release();
                }
                return menuCrafter.craft(new MinecraftScreen.MenuParameters<>(MinecraftScreen.MENU_HOLDER_MAP.get(screen.location()).value(), id, inventory, pair.getFirst(), owner));
            }, new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buf, Pair<D, byte[]> pair) {
                    NexoMinecraft.packetCodec(screen.data()).encode(buf, pair.getFirst());
                    buf.writeByteArray(pair.getSecond());
                }

                @Override
                public @NotNull Pair<D, byte[]> decode(RegistryFriendlyByteBuf buf) {
                    D data = NexoMinecraft.packetCodec(screen.data()).decode(buf);
                    return Pair.of(data, buf.readByteArray());
                }
            });
            this.menuCrafter = menuCrafter;
            this.screenCrafter = screenCrafter;
        }

        @Override
        public @NotNull MenuType<?> type() {
            return this;
        }

        @Override
        public @NotNull MinecraftScreen.ExtendedMenu<D> craftMenu(int id, @NotNull Inventory inventory, @NotNull D data, @Nullable Unit<?> owner) {
            return this.menuCrafter.craft(new MinecraftScreen.MenuParameters<>(this, id, inventory, data, owner));
        }

        @Override
        public @NotNull AbstractContainerScreen<MinecraftScreen.ExtendedMenu<D>> craftScreen(@NotNull MinecraftScreen.ExtendedMenu<D> menu, @NotNull Inventory inventory, @NotNull Component title) {
            Class<AbstractContainerScreen<MinecraftScreen.ExtendedMenu<D>>> screenType = Nexo.type(AbstractContainerScreen.class);
            Screen screen = this.screenCrafter.craft(new MinecraftScreen.ScreenParameters<>(menu, inventory, title, menu.data()));
            return screenType.cast(screen);
        }

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
                Codec<D> codec = NexoMinecraft.codec(data);
                builder.persistent(codec);
                builder.copyOnDeath();
            }
            if (data.synced()) {
                StreamCodec<RegistryFriendlyByteBuf, D> codec = NexoMinecraft.packetCodec(data);
                builder.syncWith(codec, AttachmentSyncPredicate.all());
            }
        });
        dataAttachmentMap.put(data, type);
        attachmentDataMap.put(type, data);
    }

    @Override
    protected <M> void addBuiltinRegistryListener(MinecraftFeatureType<?, M> type) {
        RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registry(type.registry()).ifPresent(registry -> {
            RegistryEntryAddedCallback.allEntries(registry, holder -> {
                emitFeatureRegistered(new FeatureRegisteredEvent(NexoMinecraft.id(holder), type.index(this.nexo(), holder)));
            });
        });
    }

    @Override
    public <T extends Feature<T, U> & VaultFactory<U>, U extends Unit<T>, M> void registerVaults(@NotNull MinecraftFeatureType<T, M> type, @NotNull T feature, @NotNull Supplier<M> minecraft) {
        Map<String, Function<U, Vault.Slotted<ItemUnit>>> factories = feature.vaults(ItemUnit.class);
        if (factories.isEmpty()) {
            return;
        }
        M value = minecraft.get();
        if (type.minecraftType() == Block.class) {
            if (!(value instanceof EntityBlock)) {
                throw new IllegalArgumentException("Blocks with item Vaults must implement EntityBlock");
            }
            ItemStorage.SIDED.registerForBlocks((world, pos, state, blockEntity, direction) -> this.createVaultCapability(feature, () -> this.createVaultStorage(this.createVaults(this.nexo().blockToUnit(world, pos, state, blockEntity, direction), factories))), (Block) value);
        } else if (type.minecraftType() == Item.class) {
            ItemStorage.ITEM.registerForItems((stack, context) -> this.createVaultCapability(feature, () -> this.createVaultStorage(this.createVaults(this.nexo().stackToUnit(stack), factories))), (Item) value);
        } else if (type.minecraftType() == EntityType.class) {
            ENTITY_ITEM_STORAGE.registerForTypes((entity, context) -> this.createVaultCapability(feature, () -> this.createVaultStorage(this.createVaults(this.nexo().entityToUnit(entity), factories))), (EntityType<?>) value);
        } else {
            throw new IllegalArgumentException("Unsupported vault feature type: " + type.minecraftType().getName());
        }
    }

    private @Nullable Storage<ItemVariant> createVaultStorage(@NotNull List<Vault.Slotted<ItemUnit>> vaults) {
        List<SlottedStorage<ItemVariant>> storages = new ArrayList<>(vaults.size());
        for (Vault.Slotted<ItemUnit> vault : vaults) {
            storages.add(new FabricVaultItemStorage(this.nexo(), vault));
        }
        return switch (storages.size()) {
            case 0 -> null;
            case 1 -> storages.getFirst();
            default -> new CombinedSlottedStorage<>(storages);
        };
    }

    public <D> @NotNull AttachmentType<D> getDataAttachment(@NotNull DataBase<D> data) {
        Class<AttachmentType<D>> clazz = Nexo.type(AttachmentType.class);
        return clazz.cast(dataAttachmentMap.get(data));
    }

    public @Nullable DataBase<?> getAttachmentData(@NotNull AttachmentType<?> type) {
        return attachmentDataMap.get(type);
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

}
