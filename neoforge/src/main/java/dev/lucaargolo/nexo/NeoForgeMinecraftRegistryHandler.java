package dev.lucaargolo.nexo;

import com.mojang.serialization.Codec;
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
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.event.DynamicRegistrySetupEvent;
import dev.lucaargolo.nexo.event.WorldDimensionsBakeEvent;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.fluid.MinecraftFluid;
import dev.lucaargolo.nexo.feature.item.MinecraftItemCategory;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import dev.lucaargolo.nexo.unit.NeoForgeVaultItemHandler;
import dev.lucaargolo.nexo.unit.screen.MinecraftScreenUnit;
import dev.lucaargolo.nexo.util.DynamicRegistryView;
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
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.callback.AddCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NeoForgeMinecraftRegistryHandler extends MinecraftRegistryHandler {

    private final Map<Registry<?>, Map<String, DeferredRegister<?>>> deferredRegistries = new HashMap<>();

    private final Map<DataBase<?>, Holder<AttachmentType<?>>> dataAttachmentMap = new LinkedHashMap<>();

    private final List<Consumer<RegisterCapabilitiesEvent>> inventoryRegistrars = new ArrayList<>();
    private final ThreadLocal<Set<Object>> activeVaultFeatures = ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

    private final Set<ResourceKey<? extends Registry<?>>> firedRegistries = ConcurrentHashMap.newKeySet();

    public NeoForgeMinecraftRegistryHandler(NeoForgeNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public void init() {
        NeoForgeNexoMinecraft nexo = (NeoForgeNexoMinecraft) this.nexo();
        nexo.modBus().addListener(RegisterEvent.class, event -> this.firedRegistries.add(event.getRegistryKey()));
        super.init();
        nexo.modBus().addListener(RegisterCapabilitiesEvent.class, event -> {
            this.inventoryRegistrars.forEach(registrar -> registrar.accept(event));
        });
        NeoForge.EVENT_BUS.addListener(DynamicRegistrySetupEvent.class, event -> {
            MinecraftFeatureType.all().stream()
                    .filter(type -> type.registryType() != MinecraftFeatureType.RegistryType.DIRECT)
                    .forEach(type -> this.addDynamicRegistryListener(event.view(), type));
            dynamicRegistrars.forEach((key, registrar) -> {
                event.view().getOptional(key.registryKey()).ifPresent(registrar);
            });
        });
        NeoForge.EVENT_BUS.addListener(WorldDimensionsBakeEvent.class, event -> {
            event.dimensions().forEach((key, stem) -> {
                Holder<LevelStem> holder = event.registry().getHolderOrThrow(key);
                MinecraftFeatureType.WORLD.index(this.nexo(), holder);
            });
        });
    }

    private <M> void addDynamicRegistryListener(DynamicRegistryView view, MinecraftFeatureType<?, M> type) {
        view.registerEntryAdded(type.registry(), (r, raw, id, value) -> {
            Holder.Reference<M> holder = view.getOptional(type.registry()).flatMap(registry -> registry.getHolder(raw)).orElseThrow();
            emitFeatureRegistered(new FeatureRegisteredEvent(NexoMinecraft.id(holder), type.index(this.nexo(), holder)));
            dynamicHolders.put(holder.key(), holder);
        });
    }

    @Override
    public <T> Holder<T> registerBuiltinFeature(Registry<T> registry, ResourceLocation id, Supplier<T> feature) {
        return this.registerDeferred(registry, id, feature);
    }

    private <R> Holder<R> registerDeferred(Registry<R> registry, ResourceLocation id, Supplier<R> feature) {
        DeferredRegister<R> deferredRegistry = getOrCreateDeferredRegister(registry, id.getNamespace());
        if (this.firedRegistries.contains(registry.key())) {
            // The RegisterEvent for this registry already fired: DeferredRegister rejects new entries after
            // that point, but the registry stays unfrozen for the whole postRegisterEvents window, so
            // register directly (this mirrors how Fabric registers builtin features).
            return Registry.registerForHolder(registry, id, feature.get());
        }
        return deferredRegistry.register(id.getPath(), feature);
    }

    @Override
    protected <T> Registry<T> createRegistry(ResourceKey<Registry<T>> registryKey) {
        DeferredRegister<T> deferredRegistry = DeferredRegister.create(registryKey, NexoMinecraft.MOD_ID);
        Registry<T> registry = deferredRegistry.makeRegistry(builder -> {
        });
        NeoForgeNexoMinecraft nexo = (NeoForgeNexoMinecraft) this.nexo();
        deferredRegistry.register(nexo.modBus());
        deferredRegistries.computeIfAbsent(registry, key -> new HashMap<>()).put(NexoMinecraft.MOD_ID, deferredRegistry);
        return registry;
    }

    @Override
    protected <M> void addBuiltinRegistryListener(MinecraftFeatureType<?, M> type) {
        RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registry(type.registry()).ifPresent(registry -> {
            Consumer<Holder<M>> consumer = (holder) -> {
                emitFeatureRegistered(new FeatureRegisteredEvent(NexoMinecraft.id(holder), type.index(this.nexo(), holder)));
            };
            registry.addCallback((AddCallback<M>) (r, raw, id, value) -> {
                // Look the holder up by key: the raw id only matches the appended byId index during
                // mod-order dispatch, not while NeoForge replays a snapshot (revertToVanilla).
                consumer.accept(r.getHolder(id).orElseThrow());
            });
            registry.holders().toList().forEach(consumer);
        });
    }

    @Override
    public <D> void registerDataAttachment(DataBase<D> data) {
        ResourceLocation id = NexoMinecraft.rl(data.location());
        AttachmentType.Builder<D> builder = AttachmentType.builder(data::initial);
        if (data.persistent()) {
            Codec<D> codec = NexoMinecraft.codec(data);
            builder.serialize(codec);
            builder.copyOnDeath();
        }
        if (data.synced()) {
            StreamCodec<RegistryFriendlyByteBuf, D> codec = NexoMinecraft.packetCodec(data);
            builder.sync(codec);
        }
        Holder<AttachmentType<?>> holder = this.registerDeferred(NeoForgeRegistries.ATTACHMENT_TYPES, id, builder::build);
        dataAttachmentMap.put(data, holder);
    }

    public <D> @NotNull AttachmentType<D> getDataAttachment(@NotNull DataBase<D> data) {
        Class<AttachmentType<D>> clazz = Nexo.type(AttachmentType.class);
        return clazz.cast(dataAttachmentMap.get(data).value());
    }

    public @NotNull List<@NotNull DataBase<?>> getAttachedData(@NotNull IAttachmentHolder target) {
        List<@NotNull DataBase<?>> data = new ArrayList<>();
        for (Map.Entry<DataBase<?>, Holder<AttachmentType<?>>> entry : dataAttachmentMap.entrySet()) {
            if (target.hasData(entry.getValue().value())) {
                data.add(entry.getKey());
            }
        }
        return data;
    }

    @Override
    public void registerFluidType(@NotNull MinecraftFluid.Entry entry) {
        ResourceLocation id = NexoMinecraft.rl(entry.location());
        this.registerDeferred(NeoForgeRegistries.FLUID_TYPES, id, () -> new FluidType(FluidType.Properties.create()
                .descriptionId(entry.location().namespace() + ".fluid." + entry.location().path().replace('/', '.'))
                .fallDistanceModifier(0.0F)
                .canExtinguish(true)
                .canConvertToSource(false)
                .canSwim(true)
                .canDrown(true)
                .supportsBoating(true)
                .density(1000)
                .temperature(300)
                .viscosity(1000)));
    }

    public @NotNull FluidType getFluidType(@NotNull MinecraftFluid.Entry entry) {
        ResourceLocation id = NexoMinecraft.rl(entry.location());
        FluidType type = NeoForgeRegistries.FLUID_TYPES.get(id);
        if (type == null) {
            throw new IllegalStateException("FluidType is not registered for fluid " + id);
        }
        return type;
    }

    @Override
    public CreativeModeTab craftCreativeTab(ItemCategoryBase category) {
        Component title = Component.translatable(category.languageKey());
        return CreativeModeTab.builder().title(title).displayItems((parameters, output) -> {
            MinecraftItemCategory.ITEM_MAP.getOrDefault(category, List.of()).forEach(item -> {
                output.accept(MinecraftFeatureType.ITEM.convert(item));
            });
        }).build();
    }

    @Override
    public <D> MinecraftScreen.ExtendedMenuType<D> craftMenuType(@NotNull ScreenBase<D> screen, @NotNull MinecraftScreen.MenuCrafter<D> menuCrafter, @NotNull MinecraftScreen.ScreenCrafter<D> screenCrafter) {
        return new ExtendedMenuTypeImpl<>(this.nexo(), screen, menuCrafter, screenCrafter);
    }

    private static final class ExtendedMenuTypeImpl<D> extends MenuType<AbstractContainerMenu> implements MinecraftScreen.ExtendedMenuType<D> {

        private final MinecraftScreen.MenuCrafter<D> menuCrafter;
        private final MinecraftScreen.ScreenCrafter<D> screenCrafter;

        public ExtendedMenuTypeImpl(@NotNull NexoMinecraft nexo, @NotNull ScreenBase<D> screen, MinecraftScreen.MenuCrafter<D> menuCrafter, MinecraftScreen.ScreenCrafter<D> screenCrafter) {
            super((IContainerFactory<AbstractContainerMenu>) (id, inventory, buf) -> {
                D data = NexoMinecraft.packetCodec(screen.data()).decode(buf);
                Unit<?> owner = MinecraftScreenUnit.decodeOwner(nexo, buf, inventory.player.level());
                return menuCrafter.craft(new MinecraftScreen.MenuParameters<>(MinecraftScreen.MENU_HOLDER_MAP.get(screen.location()).value(), id, inventory, data, owner));
            }, FeatureFlags.VANILLA_SET);
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
    public @NotNull MinecraftFluid.ExtendedFluid craftFluid(@NotNull MinecraftFluid.Entry entry, boolean source) {
        return new ExtendedFluidImpl(entry, source);
    }

    private final class ExtendedFluidImpl extends MinecraftFluid.ExtendedFluid {

        private ExtendedFluidImpl(@NotNull MinecraftFluid.Entry entry, boolean source) {
            super(entry, source);
        }

        @Override
        public @NotNull FluidType getFluidType() {
            return NeoForgeMinecraftRegistryHandler.this.getFluidType(this.entry());
        }

    }

    @Override
    public <T extends Feature<T, U> & VaultFactory<U>, U extends Unit<T> & VaultProvider, M> void registerVaults(@NotNull MinecraftFeatureType<T, M> type, @NotNull T feature, @NotNull Supplier<M> minecraft) {
        Map<String, Function<U, Vault.Slotted<ItemUnit>>> factories = feature.vaults(ItemUnit.class);
        if (factories.isEmpty()) {
            return;
        }
        if (type.minecraftType() == Block.class) {
            this.inventoryRegistrars.add(event -> {
                Block block = (Block) minecraft.get();
                if (!(block instanceof EntityBlock)) {
                    throw new IllegalArgumentException("Blocks with item Vaults must implement EntityBlock");
                }
                event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, blockEntity, context) -> this.createVaultCapability(feature, () -> this.createVaultHandler(this.createVaults(this.nexo().blockToUnit(level, pos, state, blockEntity, context), factories))), block);
            });
        } else if (type.minecraftType() == Item.class) {
            this.inventoryRegistrars.add(event -> event.registerItem(Capabilities.ItemHandler.ITEM, (stack, context) -> this.createVaultCapability(feature, () -> this.createVaultHandler(this.createVaults(this.nexo().stackToUnit(stack), factories))), (Item) minecraft.get()));
        } else if (type.minecraftType() == EntityType.class) {
            this.inventoryRegistrars.add(event -> {
                EntityType<?> entityType = Nexo.<EntityType<?>>type(EntityType.class).cast(minecraft.get());
                event.registerEntity(Capabilities.ItemHandler.ENTITY, entityType, (entity, context) -> this.createVaultCapability(feature, () -> this.createVaultHandler(this.createVaults(this.nexo().entityToUnit(entity), factories))));
            });
        } else {
            throw new IllegalArgumentException("Unsupported vault feature type: " + type.minecraftType().getName());
        }
    }

    private @Nullable IItemHandler createVaultHandler(@NotNull List<Vault.Slotted<ItemUnit>> vaults) {
        return vaults.isEmpty() ? null : new NeoForgeVaultItemHandler(this.nexo(), vaults);
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

    @Override
    protected RegistryAccess localAccess() {
        return null;
    }

    private <R> DeferredRegister<R> getOrCreateDeferredRegister(Registry<R> registry, String namespace) {
        DeferredRegister<?> deferredRegister = deferredRegistries
                .computeIfAbsent(registry, r -> new HashMap<>())
                .computeIfAbsent(namespace, n -> {
                    DeferredRegister<R> r = DeferredRegister.create(registry, namespace);
                    r.register(((NeoForgeNexoMinecraft) this.nexo()).modBus());
                    return r;
                });
        Class<DeferredRegister<R>> clazz = Nexo.type(DeferredRegister.class);
        return clazz.cast(deferredRegister);
    }

}
