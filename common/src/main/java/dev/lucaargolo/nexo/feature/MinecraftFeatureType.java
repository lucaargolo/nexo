package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.world.BiomeBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemCategoryUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.block.MinecraftBlock;
import dev.lucaargolo.nexo.feature.data.MinecraftData;
import dev.lucaargolo.nexo.feature.entity.MinecraftEntity;
import dev.lucaargolo.nexo.feature.item.MinecraftItem;
import dev.lucaargolo.nexo.feature.item.MinecraftItemCategory;
import dev.lucaargolo.nexo.feature.world.MinecraftBiome;
import dev.lucaargolo.nexo.feature.world.MinecraftWorld;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.unit.block.MinecraftBlockUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemCategoryUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import dev.lucaargolo.nexo.util.Bijection;
import dev.lucaargolo.nexo.util.NexoUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MinecraftFeatureType<T extends Feature<T, U>, U extends Unit<T, ?>, M> {

    private static final Map<Feature.Type<?, ?>, MinecraftFeatureType<?, ?, ?>> TYPES = new HashMap<>();

    public static final MinecraftFeatureType<DataBase<?>, Unit<DataBase<?>, ?>, DataComponentType<?>> DATA = new MinecraftFeatureType<>(
            Nexo.type(DataComponentType.class),
            Feature.Type.data(),
            Registries.DATA_COMPONENT_TYPE,
            MinecraftData::register,
            MinecraftData::index,
            MinecraftData::lookup,
            MinecraftData.CONVERT,
            Map.of(DataComponentType.class, MinecraftFeatureType.<DataBase<?>, DataComponentType<?>>direct(Nexo.type(DataComponentType.class), MinecraftData::craft))
    );

    public static final MinecraftFeatureType<BlockBase, BlockUnit<?>, Block> BLOCK = new MinecraftFeatureType<>(
            Block.class,
            Feature.Type.BLOCK,
            Registries.BLOCK,
            MinecraftBlock::register,
            MinecraftBlock::index,
            MinecraftBlock::lookup,
            MinecraftBlock.CONVERT,
            Map.of(Block.class, extensible(Block.class, Block.class, BlockBehaviour.Properties.class, MinecraftBlock::craft)),
            (helper, feature, block) -> NexoUtils.loadPlatformClass(helper.nexo(), MinecraftBlockUnit.class, helper, feature, feature.role(), null, null, block.defaultBlockState(), null)
    );

    public static final MinecraftFeatureType<ItemBase, ItemUnit<?>, Item> ITEM = new MinecraftFeatureType<>(
            Item.class,
            Feature.Type.ITEM,
            Registries.ITEM,
            MinecraftItem::register,
            MinecraftItem::index,
            MinecraftItem::lookup,
            MinecraftItem.CONVERT,
            Map.of(Item.class, extensible(Item.class, Item.class, Item.Properties.class, MinecraftItem::craft)),
            (helper, feature, item) -> new MinecraftItemUnit<>(helper.nexo(), feature, feature.role(), item.getDefaultInstance())
    );

    public static final MinecraftFeatureType<ItemCategoryBase, ItemCategoryUnit<?>, CreativeModeTab> ITEM_CATEGORY = new MinecraftFeatureType<>(
            CreativeModeTab.class,
            Feature.Type.ITEM_CATEGORY,
            Registries.CREATIVE_MODE_TAB,
            MinecraftItemCategory::register,
            MinecraftItemCategory::index,
            MinecraftItemCategory::lookup,
            MinecraftItemCategory.CONVERT,
            Map.of(CreativeModeTab.class, direct(CreativeModeTab.class, MinecraftItemCategory::craft)),
            (helper, feature, tab) -> NexoUtils.loadPlatformClass(helper.nexo(), MinecraftItemCategoryUnit.class, helper, feature, feature.role(), tab)
    );

    public static final MinecraftFeatureType<EntityBase, EntityUnit<?>, EntityType<?>> ENTITY = new MinecraftFeatureType<>(
            Nexo.type(EntityType.class),
            Feature.Type.ENTITY,
            Registries.ENTITY_TYPE,
            MinecraftEntity::register,
            MinecraftEntity::index,
            MinecraftEntity::lookup,
            MinecraftEntity.CONVERT,
            Map.of(EntityType.class, extensible(Nexo.type(EntityType.class), Entity.class, MinecraftEntity.Parameters.class, MinecraftEntity::craft))
    );

    public static final MinecraftFeatureType<WorldBase, WorldUnit<?>, LevelStem> WORLD = new MinecraftFeatureType<>(
            LevelStem.class,
            Feature.Type.WORLD,
            Registries.LEVEL_STEM,
            MinecraftWorld::register,
            MinecraftWorld::index,
            MinecraftWorld::lookup,
            MinecraftWorld.CONVERT,
            Map.of(DimensionType.class, direct(DimensionType.class, MinecraftWorld::craftType), LevelStem.class, direct(LevelStem.class, MinecraftWorld::craftStem))
    );

    public static final MinecraftFeatureType<BiomeBase, Unit<BiomeBase, ?>, Biome> BIOME = new MinecraftFeatureType<>(
            Biome.class,
            Feature.Type.BIOME,
            Registries.BIOME,
            MinecraftBiome::register,
            MinecraftBiome::index,
            MinecraftBiome::lookup,
            MinecraftBiome.CONVERT,
            Map.of(Biome.class, direct(Biome.class, MinecraftBiome::craft))
    );

    private final Class<M> minecraftType;
    private final Feature.Type<T, U> type;
    private final ResourceKey<? extends Registry<M>> registry;
    private final BiFunction<NexoRegistryHandler<?>, T, T> registrar;
    private final BiFunction<NexoRegistryHandler<?>, Holder<M>, T> index;
    private final Function<Location, T> lookup;
    private final Bijection<T, Holder<M>> convert;
    private final Map<Class<?>, CraftStrategy<T>> crafters;
    private final @Nullable MinecraftFeatureType.UnitCrafter<T, U, M> unitCrafter;

    private MinecraftFeatureType(
            Class<M> minecraftType,
            Feature.Type<T, U> type,
            ResourceKey<? extends Registry<M>> registry,
            BiFunction<NexoRegistryHandler<?>, T, T> registrar,
            BiFunction<NexoRegistryHandler<?>, Holder<M>, T> index,
            Function<Location, T> lookup,
            Bijection<T, Holder<M>> convert,
            Map<Class<?>, CraftStrategy<T>> crafters
    ) {
        this(minecraftType, type, registry, registrar, index, lookup, convert, crafters, null);
    }

    private MinecraftFeatureType(
            Class<M> minecraftType,
            Feature.Type<T, U> type,
            ResourceKey<? extends Registry<M>> registry,
            BiFunction<NexoRegistryHandler<?>, T, T> registrar,
            BiFunction<NexoRegistryHandler<?>, Holder<M>, T> index,
            Function<Location, T> lookup,
            Bijection<T, Holder<M>> convert,
            Map<Class<?>, CraftStrategy<T>> crafters,
            @Nullable MinecraftFeatureType.UnitCrafter<T, U, M> unitCrafter
    ) {
        this.minecraftType = minecraftType;
        this.type = type;
        this.registry = registry;
        this.registrar = registrar;
        this.index = index;
        this.lookup = lookup;
        this.convert = convert;
        this.crafters = crafters;
        this.unitCrafter = unitCrafter;
        TYPES.put(type, this);
    }

    public boolean isInstance(Feature<?, ?> feature) {
        return this.type.isInstance(feature);
    }

    public ResourceKey<? extends Registry<M>> registry() {
        return registry;
    }

    public @NotNull T register(NexoRegistryHandler<?> helper, Feature<?, ?> feature) {
        return registrar.apply(helper, type.cast(feature));
    }

    public @NotNull T index(NexoRegistryHandler<?> helper, Holder<M> holder) {
        return index.apply(helper, holder);
    }

    public @Nullable T lookup(Location location) {
        return lookup.apply(location);
    }

    public @NotNull M convert(T feature) {
        return convert.forward(feature).value();
    }

    public @NotNull T convert(NexoRegistryHandler<?> helper, M feature) {
        return convert.backward(holder(helper, feature));
    }

    public @NotNull Supplier<M> craft(NexoRegistryHandler<?> helper, T feature) {
        return craft(helper, minecraftType, feature);
    }

    public @NotNull <MM> Supplier<MM> craft(NexoRegistryHandler<?> helper, Class<MM> minecraftType, T feature) {
        return () -> {
            CraftStrategy<T> strategy = this.crafters.get(minecraftType);
            if (strategy == null) {
                throw new UnsupportedOperationException("Unsupported feature crafter: " + minecraftType.getName());
            }
            return minecraftType.cast(strategy.craft(helper, feature));
        };
    }

    public @Nullable U unit(
            @NotNull NexoRegistryHandler<?> helper,
            @NotNull Feature<?, ?> feature
    ) {
        if (unitCrafter != null) {
            T value = type.cast(feature);
            return unitCrafter.craft(helper, value, convert(value));
        }else {
            return null;
        }
    }

    private Holder<M> holder(NexoRegistryHandler<?> helper, M feature) {
        RegistryAccess access = helper.getRegistry();
        Registry<M> registry = access.registryOrThrow(this.registry);
        ResourceKey<M> key = registry.getResourceKey(feature).orElseThrow();
        return registry.getHolderOrThrow(key);
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

    private static <T extends Feature<T, ?>, M> CraftStrategy<T> direct(Class<M> minecraftType, DirectCrafter<T, M> crafter) {
        return new DirectStrategy<>(minecraftType, crafter);
    }

    private static <T extends Feature<T, ?>, M, E, P> CraftStrategy<T> extensible(Class<M> minecraftType, Class<E> extensionType, Class<P> parameterType, ExtensibleCrafter<T, M, E, P> crafter) {
        if (!NexoUtils.isExtendable(extensionType)) {
            throw new IllegalArgumentException(extensionType.getName() + " is not extendable");
        }
        return new ExtensibleStrategy<>(minecraftType, extensionType, parameterType, crafter);
    }

    private interface CraftStrategy<T extends Feature<T, ?>> {
        @NotNull Object craft(@NotNull NexoRegistryHandler<?> helper, @NotNull T feature);
    }

    private record DirectStrategy<T extends Feature<T, ?>, M>(Class<M> minecraftType, DirectCrafter<T, M> crafter) implements CraftStrategy<T> {
        @Override
        public @NotNull M craft(@NotNull NexoRegistryHandler<?> helper, @NotNull T feature) {
            return crafter.craft(helper, feature);
        }
    }

    private record ExtensibleStrategy<T extends Feature<T, ?>, M, E, P>(Class<M> minecraftType, Class<E> extensionType, Class<P> parameterType, ExtensibleCrafter<T, M, E, P> crafter) implements CraftStrategy<T> {
        @Override
        public @NotNull M craft(@NotNull NexoRegistryHandler<?> helper, @NotNull T feature) {
            MinecraftRoleType.Info<E, P> info = MinecraftRoleType.craft(helper, feature);
            if (info == null) {
                info = new MinecraftRoleType.Info<>(NexoUtils.extend(helper.nexo(), extensionType), null);
            }
            NexoUtils.Extender<E> extender = info.extender();
            return crafter.craft(helper, extender, info.factory(), feature);
        }
    }

    @FunctionalInterface
    private interface DirectCrafter<T extends Feature<T, ?>, M> {
        @NotNull M craft(@NotNull NexoRegistryHandler<?> helper, @NotNull T feature);
    }

    @FunctionalInterface
    private interface ExtensibleCrafter<T extends Feature<T, ?>, M, E, P> {
        @NotNull M craft(@NotNull NexoRegistryHandler<?> helper, @NotNull NexoUtils.Extender<E> extender, @Nullable Function<P, E> factory, @NotNull T feature);
    }

    @FunctionalInterface
    private interface UnitCrafter<T extends Feature<T, U>, U extends Unit<T, ?>, M> {
        @NotNull U craft(
                @NotNull NexoRegistryHandler<?> helper,
                @NotNull T feature,
                @NotNull M minecraft
        );
    }

}

