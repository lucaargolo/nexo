package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.packet.Packet;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.feature.world.BiomeBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemCategoryUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.block.MinecraftBlock;
import dev.lucaargolo.nexo.feature.data.MinecraftData;
import dev.lucaargolo.nexo.feature.entity.MinecraftEntity;
import dev.lucaargolo.nexo.feature.item.MinecraftItem;
import dev.lucaargolo.nexo.feature.item.MinecraftItemCategory;
import dev.lucaargolo.nexo.feature.packet.MinecraftPacket;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import dev.lucaargolo.nexo.feature.world.MinecraftBiome;
import dev.lucaargolo.nexo.feature.world.MinecraftWorld;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Bijection;
import dev.lucaargolo.nexo.util.Utils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MinecraftFeatureType<T extends Feature<T, U>, U extends Unit<T, ?>, M> {

    public enum RegistryType {
        BUILTIN,
        CUSTOM,
        DIRECT
    }

    private static final Map<Feature.Type<?, ?>, MinecraftFeatureType<?, ?, ?>> TYPES = new HashMap<>();

    public static final MinecraftFeatureType<DataBase<?>, Unit<DataBase<?>, ?>, DataComponentType<?>> DATA = MinecraftFeatureType.base(
            Nexo.type(DataComponentType.class),
            Feature.Type.data(),
            Registries.DATA_COMPONENT_TYPE,
            MinecraftData::register,
            MinecraftData::index,
            MinecraftData::lookup,
            MinecraftData.CONVERT,
            Map.of(DataComponentType.class, CraftStrategy.<DataBase<?>, DataComponentType<?>>direct(Nexo.type(DataComponentType.class), MinecraftData::craft))
    );

    public static final MinecraftFeatureType<Packet<?, ?>, Unit<Packet<?, ?>, ?>, Packet<?, ?>> PACKET = MinecraftFeatureType.custom(
            Nexo.type(Packet.class),
            Feature.Type.packet(),
            MinecraftPacket.REGISTRY,
            MinecraftPacket::register,
            MinecraftPacket::index,
            MinecraftPacket::lookup,
            MinecraftPacket.CONVERT,
            Map.of(Packet.class, CraftStrategy.direct(Nexo.type(Packet.class), MinecraftPacket::craft))
    );

    public static final MinecraftFeatureType<BlockBase, BlockUnit<?>, Block> BLOCK = MinecraftFeatureType.base(
            Block.class,
            Feature.Type.BLOCK,
            Registries.BLOCK,
            MinecraftBlock::register,
            MinecraftBlock::index,
            MinecraftBlock::lookup,
            MinecraftBlock.CONVERT,
            Map.of(Block.class, CraftStrategy.extensible(Block.class, Block.class, BlockBehaviour.Properties.class, MinecraftBlock::craft)),
            (nexo, feature, block) -> nexo.stateToUnit(block.defaultBlockState())
    );

    public static final MinecraftFeatureType<ItemBase, ItemUnit<?>, Item> ITEM = MinecraftFeatureType.base(
            Item.class,
            Feature.Type.ITEM,
            Registries.ITEM,
            MinecraftItem::register,
            MinecraftItem::index,
            MinecraftItem::lookup,
            MinecraftItem.CONVERT,
            Map.of(Item.class, CraftStrategy.extensible(Item.class, Item.class, Item.Properties.class, MinecraftItem::craft)),
            (nexo, feature, item) -> nexo.stackToUnit(item.getDefaultInstance())
    );

    public static final MinecraftFeatureType<ItemCategoryBase, ItemCategoryUnit<?>, CreativeModeTab> ITEM_CATEGORY = MinecraftFeatureType.base(
            CreativeModeTab.class,
            Feature.Type.ITEM_CATEGORY,
            Registries.CREATIVE_MODE_TAB,
            MinecraftItemCategory::register,
            MinecraftItemCategory::index,
            MinecraftItemCategory::lookup,
            MinecraftItemCategory.CONVERT,
            Map.of(CreativeModeTab.class, CraftStrategy.direct(CreativeModeTab.class, MinecraftItemCategory::craft)),
            (nexo, feature, tab) -> nexo.tabToUnit(tab)
    );

    public static final MinecraftFeatureType<EntityBase, EntityUnit<?>, EntityType<?>> ENTITY = MinecraftFeatureType.base(
            Nexo.type(EntityType.class),
            Feature.Type.ENTITY,
            Registries.ENTITY_TYPE,
            MinecraftEntity::register,
            MinecraftEntity::index,
            MinecraftEntity::lookup,
            MinecraftEntity.CONVERT,
            Map.of(EntityType.class, CraftStrategy.extensible(Nexo.type(EntityType.class), Entity.class, MinecraftEntity.Parameters.class, MinecraftEntity::craft))
    );

    public static final MinecraftFeatureType<WorldBase, WorldUnit<?>, LevelStem> WORLD = MinecraftFeatureType.base(
            LevelStem.class,
            Feature.Type.WORLD,
            Registries.LEVEL_STEM,
            MinecraftWorld::register,
            MinecraftWorld::index,
            MinecraftWorld::lookup,
            MinecraftWorld.CONVERT,
            Map.of(DimensionType.class, CraftStrategy.direct(DimensionType.class, MinecraftWorld::craftType), LevelStem.class, CraftStrategy.direct(LevelStem.class, MinecraftWorld::craftStem))
    );

    public static final MinecraftFeatureType<BiomeBase, Unit<BiomeBase, ?>, Biome> BIOME = MinecraftFeatureType.base(
            Biome.class,
            Feature.Type.BIOME,
            Registries.BIOME,
            MinecraftBiome::register,
            MinecraftBiome::index,
            MinecraftBiome::lookup,
            MinecraftBiome.CONVERT,
            Map.of(Biome.class, CraftStrategy.direct(Biome.class, MinecraftBiome::craft))
    );

    public static final MinecraftFeatureType<ScreenBase, ScreenUnit<?>, Screen> SCREEN = MinecraftFeatureType.direct(
            Screen.class,
            Feature.Type.SCREEN,
            MinecraftScreen::register,
            MinecraftScreen::lookup,
            MinecraftScreen.CONVERT,
            Map.of(Screen.class, CraftStrategy.extensible(Screen.class, Screen.class, Component.class, MinecraftScreen::craft)),
            (nexo, feature, screen) -> nexo.screenToUnit(screen)
    );

    private final @NotNull Class<M> minecraftType;
    private final @NotNull Feature.Type<T, U> type;

    private final @Nullable ResourceKey<Registry<M>> registry;
    private final @NotNull RegistryType registryType;

    private final @NotNull BiFunction<NexoMinecraft<?, ?, ?, ?>, T, T> registrar;
    private final @Nullable BiFunction<NexoMinecraft<?, ?, ?, ?>, Holder<M>, T> index;
    private final @NotNull Function<Location, T> lookup;

    private final @Nullable Bijection<T, Holder<M>> holderConvert;
    private final @Nullable Bijection<T, M> directConvert;

    private final @NotNull Map<Class<?>, CraftStrategy<T>> crafters;
    private final @Nullable MinecraftFeatureType.UnitCrafter<T, U, M> unitCrafter;

    private MinecraftFeatureType(
            @NotNull Class<M> minecraftType,
            @NotNull Feature.Type<T, U> type,
            @Nullable ResourceKey<Registry<M>> registry,
            @NotNull RegistryType registryType,
            @NotNull BiFunction<NexoMinecraft<?, ?, ?, ?>, T, T> registrar,
            @Nullable BiFunction<NexoMinecraft<?, ?, ?, ?>, Holder<M>, T> index,
            @NotNull Function<Location, T> lookup,
            @Nullable Bijection<T, Holder<M>> holderConvert,
            @Nullable Bijection<T, M> directConvert,
            @NotNull Map<Class<?>, CraftStrategy<T>> crafters,
            @Nullable MinecraftFeatureType.UnitCrafter<T, U, M> unitCrafter
    ) {
        this.minecraftType = minecraftType;
        this.type = type;
        this.registry = registry;
        this.registryType = registryType;
        this.registrar = registrar;
        this.index = index;
        this.lookup = lookup;
        this.holderConvert = holderConvert;
        this.directConvert = directConvert;
        this.crafters = crafters;
        this.unitCrafter = unitCrafter;
        TYPES.put(type, this);
    }

    private static <T extends Feature<T, U>, U extends Unit<T, ?>, M> MinecraftFeatureType<T, U, M> base(
            @NotNull Class<M> minecraftType,
            @NotNull Feature.Type<T, U> type,
            @NotNull ResourceKey<Registry<M>> registry,
            @NotNull BiFunction<NexoMinecraft<?, ?, ?, ?>, T, T> registrar,
            @NotNull BiFunction<NexoMinecraft<?, ?, ?, ?>, Holder<M>, T> holderIndex,
            @NotNull Function<Location, T> lookup,
            @NotNull Bijection<T, Holder<M>> holderConvert,
            @NotNull Map<Class<?>, CraftStrategy<T>> crafters
    ) {
        return new MinecraftFeatureType<>(minecraftType, type, registry, RegistryType.BUILTIN, registrar, holderIndex, lookup, holderConvert, null, crafters, null);
    }

    private static <T extends Feature<T, U>, U extends Unit<T, ?>, M> MinecraftFeatureType<T, U, M> base(
            @NotNull Class<M> minecraftType,
            @NotNull Feature.Type<T, U> type,
            @NotNull ResourceKey<Registry<M>> registry,
            @NotNull BiFunction<NexoMinecraft<?, ?, ?, ?>, T, T> registrar,
            @NotNull BiFunction<NexoMinecraft<?, ?, ?, ?>, Holder<M>, T> holderIndex,
            @NotNull Function<Location, T> lookup,
            @NotNull Bijection<T, Holder<M>> holderConvert,
            @NotNull Map<Class<?>, CraftStrategy<T>> crafters,
            @NotNull MinecraftFeatureType.UnitCrafter<T, U, M> unitCrafter
    ) {
        return new MinecraftFeatureType<>(minecraftType, type, registry, RegistryType.BUILTIN, registrar, holderIndex, lookup, holderConvert, null, crafters, unitCrafter);
    }

    private static <T extends Feature<T, U>, U extends Unit<T, ?>, M> MinecraftFeatureType<T, U, M> custom(
            @NotNull Class<M> minecraftType,
            @NotNull Feature.Type<T, U> type,
            @NotNull ResourceKey<Registry<M>> registry,
            @NotNull BiFunction<NexoMinecraft<?, ?, ?, ?>, T, T> registrar,
            @NotNull BiFunction<NexoMinecraft<?, ?, ?, ?>, Holder<M>, T> holderIndex,
            @NotNull Function<Location, T> lookup,
            @NotNull Bijection<T, Holder<M>> holderConvert,
            @NotNull Map<Class<?>, CraftStrategy<T>> crafters
    ) {
        return new MinecraftFeatureType<>(minecraftType, type, registry, RegistryType.CUSTOM, registrar, holderIndex, lookup, holderConvert, null, crafters, null);
    }

    private static <T extends Feature<T, U>, U extends Unit<T, ?>, M> MinecraftFeatureType<T, U, M> direct(
            @NotNull Class<M> minecraftType,
            @NotNull Feature.Type<T, U> type,
            @NotNull BiFunction<NexoMinecraft<?, ?, ?, ?>, T, T> registrar,
            @NotNull Function<Location, T> lookup,
            @NotNull Bijection<T, M> directConvert,
            @NotNull Map<Class<?>, CraftStrategy<T>> crafters,
            @NotNull MinecraftFeatureType.UnitCrafter<T, U, M> unitCrafter
    ) {
        return new MinecraftFeatureType<>(minecraftType, type, null, RegistryType.DIRECT, registrar, null, lookup, null, directConvert, crafters, unitCrafter);
    }

    public boolean isInstance(Feature<?, ?> feature) {
        return this.type.isInstance(feature);
    }

    public ResourceKey<Registry<M>> registry() {
        return registry;
    }

    public Class<M> minecraftType() {
        return minecraftType;
    }

    public @NotNull RegistryType registryType() {
        return registryType;
    }

    public @NotNull T register(NexoMinecraft<?, ?, ?, ?> nexo, Feature<?, ?> feature) {
        T registered = registrar.apply(nexo, type.cast(feature));
        if (registryType == RegistryType.DIRECT) {
            nexo.directRegistry.computeIfAbsent(type, ignored -> new ConcurrentHashMap<>()).put(registered.location(), registered);
        }
        return registered;
    }

    public @NotNull T index(NexoMinecraft<?, ?, ?, ?> nexo, Holder<M> holder) {
        if(index == null) {
            throw new UnsupportedOperationException("Direct feature types do not support indexing");
        }
        return index.apply(nexo, holder);
    }

    public @Nullable T lookup(Location location) {
        return lookup.apply(location);
    }

    public @Nullable T lookup(NexoMinecraft<?, ?, ?, ?> nexo, Location location) {
        if (registryType == RegistryType.DIRECT) {
            Map<Location, Feature<?, ?>> registry = nexo.directRegistry.get(type);
            return registry == null ? null : type.cast(registry.get(location));
        }
        return lookup(location);
    }

    public @NotNull M convert(T feature) {
        if(holderConvert != null) {
            return holderConvert.forward(feature).value();
        }
        if(directConvert != null) {
            return directConvert.forward(feature);
        }
        throw new IllegalStateException("Feature type has no converter");
    }

    public @NotNull T convert(NexoMinecraft<?, ?, ?, ?> nexo, M feature) {
        if(holderConvert != null) {
            if(this.registry == null) {
                throw new IllegalStateException("Non direct feature type has no registry");
            }
            RegistryAccess access = nexo.getRegistryHandler().getRegistry();
            Registry<M> registry = access.registryOrThrow(this.registry);
            ResourceKey<M> key = registry.getResourceKey(feature).orElseThrow();
            return holderConvert.backward(registry.getHolderOrThrow(key));
        }
        if(directConvert != null) {
            return directConvert.backward(feature);
        }
        throw new IllegalStateException("Feature type has no converter");
    }

    public @NotNull Supplier<M> craft(NexoMinecraft<?, ?, ?, ?> nexo, T feature) {
        return craft(nexo, minecraftType, feature);
    }

    public @NotNull <MM> Supplier<MM> craft(NexoMinecraft<?, ?, ?, ?> nexo, Class<MM> minecraftType, T feature) {
        return () -> {
            CraftStrategy<T> strategy = this.crafters.get(minecraftType);
            if (strategy == null) {
                throw new UnsupportedOperationException("Unsupported feature crafter: " + minecraftType.getName());
            }
            return minecraftType.cast(strategy.craft(nexo, feature));
        };
    }

    public @Nullable U base(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull Feature<?, ?> feature
    ) {
        if (unitCrafter != null) {
            T value = type.cast(feature);
            return unitCrafter.craft(nexo, value, convert(value));
        }
        return null;
    }

    public static <T extends Feature<T, U>, U extends Unit<T, ?>> @NotNull MinecraftFeatureType<T, U, ?> of(Feature.Type<T, U> type) {
        Class<MinecraftFeatureType<T, U, ?>> clazz = Nexo.type(MinecraftFeatureType.class);
        MinecraftFeatureType<?, ?, ?> featureType = TYPES.get(type);
        if (featureType == null) {
            throw new UnsupportedOperationException("Unsupported feature type: " + type);
        }
        return clazz.cast(featureType);
    }

    public static @NotNull Collection<MinecraftFeatureType<?, ?, ?>> all() {
        return TYPES.values();
    }



    private interface CraftStrategy<T extends Feature<T, ?>> {
        @NotNull Object craft(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull T feature);

        private static <T extends Feature<T, ?>, M> CraftStrategy<T> direct(Class<M> minecraftType, DirectCrafter<T, M> crafter) {
            return new DirectStrategy<>(minecraftType, crafter);
        }

        private static <T extends Feature<T, ?>, M, E, P> CraftStrategy<T> extensible(Class<M> minecraftType, Class<E> extensionType, Class<P> parameterType, ExtensibleCrafter<T, M, E, P> crafter) {
            if (!Utils.isExtendable(extensionType)) {
                throw new IllegalArgumentException(extensionType.getName() + " is not extendable");
            }
            return new ExtensibleStrategy<>(minecraftType, extensionType, parameterType, crafter);
        }

    }

    private record DirectStrategy<T extends Feature<T, ?>, M>(Class<M> minecraftType, DirectCrafter<T, M> crafter) implements CraftStrategy<T> {
        @Override
        public @NotNull M craft(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull T feature) {
            return crafter.craft(nexo, feature);
        }
    }

    private record ExtensibleStrategy<T extends Feature<T, ?>, M, E, P>(Class<M> minecraftType, Class<E> extensionType, Class<P> parameterType, ExtensibleCrafter<T, M, E, P> crafter) implements CraftStrategy<T> {
        @Override
        public @NotNull M craft(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull T feature) {
            MinecraftRoleType.Info<E, P> info = MinecraftRoleType.craft(nexo, feature);
            if (info == null) {
                info = new MinecraftRoleType.Info<>(Utils.extend(nexo, extensionType), null);
            }
            Utils.Extender<E> extender = info.extender();
            return crafter.craft(nexo, extender, info.factory(), feature);
        }
    }

    @FunctionalInterface
    private interface DirectCrafter<T extends Feature<T, ?>, M> {
        @NotNull M craft(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull T feature);
    }

    @FunctionalInterface
    private interface ExtensibleCrafter<T extends Feature<T, ?>, M, E, P> {
        @NotNull M craft(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Utils.Extender<E> extender, @Nullable Function<P, E> factory, @NotNull T feature);
    }

    @FunctionalInterface
    private interface UnitCrafter<T extends Feature<T, U>, U extends Unit<T, ?>, M> {
        @NotNull U craft(
                @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
                @NotNull T feature,
                @NotNull M minecraft
        );
    }

}

