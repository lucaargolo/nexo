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
import dev.lucaargolo.nexo.api.util.Pair;
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

public class MinecraftFeatureType<T extends Feature<T, U>, U extends Unit<T, ?>, M, P> {

    private static final Map<Feature.Type<?, ?>, MinecraftFeatureType<?, ?, ?, ?>> TYPES = new HashMap<>();

    public static final MinecraftFeatureType<DataBase<?>, Unit<DataBase<?>, ?>, DataComponentType<?>, Void> DATA = new MinecraftFeatureType<>(
            Nexo.type(DataComponentType.class), Nexo.type(DataComponentType.Builder.class),
            Feature.Type.data(),
            Registries.DATA_COMPONENT_TYPE,
            MinecraftData::register,
            MinecraftData::index,
            MinecraftData::lookup,
            MinecraftData.CONVERT,
            Map.of(new Pair<>(DataComponentType.class, Void.class), MinecraftFeatureType.<DataBase<Object>, DataComponentType<Object>, DataComponentType.Builder<Object>>crafter(MinecraftData::craft))
    );

    public static final MinecraftFeatureType<BlockBase, BlockUnit<?>, Block, BlockBehaviour.Properties> BLOCK = new MinecraftFeatureType<>(
            Block.class, BlockBehaviour.Properties.class,
            Feature.Type.BLOCK,
            Registries.BLOCK,
            MinecraftBlock::register,
            MinecraftBlock::index,
            MinecraftBlock::lookup,
            MinecraftBlock.CONVERT,
            Map.of(new Pair<>(Block.class, BlockBehaviour.Properties.class), MinecraftFeatureType.<BlockBase, Block, BlockBehaviour.Properties>crafter(MinecraftBlock::craft)),
            (helper, feature, block) -> NexoUtils.loadPlatformClass(helper.nexo(), MinecraftBlockUnit.class, helper, feature, feature.role(), null, null, block.defaultBlockState(), null)
    );

    public static final MinecraftFeatureType<ItemBase, ItemUnit<?>, Item, Item.Properties> ITEM = new MinecraftFeatureType<>(
            Item.class, Item.Properties.class,
            Feature.Type.ITEM,
            Registries.ITEM,
            MinecraftItem::register,
            MinecraftItem::index,
            MinecraftItem::lookup,
            MinecraftItem.CONVERT,
            Map.of(new Pair<>(Item.class, Item.Properties.class), MinecraftFeatureType.<ItemBase, Item, Item.Properties>crafter(MinecraftItem::craft)),
            (helper, feature, item) -> new MinecraftItemUnit<>(helper.nexo(), feature, feature.role(), item.getDefaultInstance())
    );

    public static final MinecraftFeatureType<ItemCategoryBase, ItemCategoryUnit<?>, CreativeModeTab, Void> ITEM_CATEGORY = new MinecraftFeatureType<>(
            CreativeModeTab.class, Void.class,
            Feature.Type.ITEM_CATEGORY,
            Registries.CREATIVE_MODE_TAB,
            MinecraftItemCategory::register,
            MinecraftItemCategory::index,
            MinecraftItemCategory::lookup,
            MinecraftItemCategory.CONVERT,
            Map.of(new Pair<>(CreativeModeTab.class, Void.class), MinecraftFeatureType.<ItemCategoryBase, CreativeModeTab, Void>crafter(MinecraftItemCategory::craft)),
            (helper, feature, tab) -> NexoUtils.loadPlatformClass(helper.nexo(), MinecraftItemCategoryUnit.class, helper, feature, feature.role(), tab)
    );

    public static final MinecraftFeatureType<EntityBase, EntityUnit<?>, EntityType<?>, Void> ENTITY = new MinecraftFeatureType<>(
            Nexo.type(EntityType.class), Void.class,
            Feature.Type.ENTITY,
            Registries.ENTITY_TYPE,
            MinecraftEntity::register,
            MinecraftEntity::index,
            MinecraftEntity::lookup,
            MinecraftEntity.CONVERT,
            Map.of(new Pair<>(EntityType.class, Void.class), MinecraftFeatureType.<EntityBase, EntityType<?>, Void>crafter(MinecraftEntity::craft))
    );

    public static final MinecraftFeatureType<WorldBase, WorldUnit<?>, LevelStem, Void> WORLD = new MinecraftFeatureType<>(
            LevelStem.class, Void.class,
            Feature.Type.WORLD,
            Registries.LEVEL_STEM,
            MinecraftWorld::register,
            MinecraftWorld::index,
            MinecraftWorld::lookup,
            MinecraftWorld.CONVERT,
            Map.of(new Pair<>(DimensionType.class, Void.class), MinecraftFeatureType.<WorldBase, DimensionType, Void>crafter(MinecraftWorld::craftType), new Pair<>(LevelStem.class, Void.class), MinecraftFeatureType.<WorldBase, LevelStem, Void>crafter(MinecraftWorld::craftStem))
    );

    public static final MinecraftFeatureType<BiomeBase, Unit<BiomeBase, ?>, Biome, Void> BIOME = new MinecraftFeatureType<>(
            Biome.class, Void.class,
            Feature.Type.BIOME,
            Registries.BIOME,
            MinecraftBiome::register,
            MinecraftBiome::index,
            MinecraftBiome::lookup,
            MinecraftBiome.CONVERT,
            Map.of(new Pair<>(Biome.class, Void.class), MinecraftFeatureType.<BiomeBase, Biome, Void>crafter(MinecraftBiome::craft))
    );

    private final Class<M> minecraftType;
    private final Class<P> parameterType;
    private final Feature.Type<T, U> type;
    private final ResourceKey<? extends Registry<M>> registry;
    private final BiFunction<NexoRegistryHandler<?>, T, T> registrar;
    private final BiFunction<NexoRegistryHandler<?>, Holder<M>, T> index;
    private final Function<Location, T> lookup;
    private final Bijection<T, Holder<M>> convert;
    private final Map<Pair<Class<?>, Class<?>>, FeatureCrafter<?, ?, ?>> crafters;
    private final @Nullable MinecraftFeatureType.UnitCrafter<T, U, M> unitCrafter;

    private MinecraftFeatureType(
            Class<M> minecraftType,
            Class<P> parameterType,
            Feature.Type<T, U> type,
            ResourceKey<? extends Registry<M>> registry,
            BiFunction<NexoRegistryHandler<?>, T, T> registrar,
            BiFunction<NexoRegistryHandler<?>, Holder<M>, T> index,
            Function<Location, T> lookup,
            Bijection<T, Holder<M>> convert,
            Map<Pair<Class<?>, Class<?>>, FeatureCrafter<?, ?, ?>> crafters
    ) {
        this(minecraftType, parameterType, type, registry, registrar, index, lookup, convert, crafters, null);
    }

    private MinecraftFeatureType(
            Class<M> minecraftType,
            Class<P> parameterType,
            Feature.Type<T, U> type,
            ResourceKey<? extends Registry<M>> registry,
            BiFunction<NexoRegistryHandler<?>, T, T> registrar,
            BiFunction<NexoRegistryHandler<?>, Holder<M>, T> index,
            Function<Location, T> lookup,
            Bijection<T, Holder<M>> convert,
            Map<Pair<Class<?>, Class<?>>, FeatureCrafter<?, ?, ?>> crafters,
            @Nullable MinecraftFeatureType.UnitCrafter<T, U, M> unitCrafter
    ) {
        this.minecraftType = minecraftType;
        this.parameterType = parameterType;
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
        return craft(helper, minecraftType, parameterType, feature);
    }

    public @NotNull <MM, MP> Supplier<MM> craft(NexoRegistryHandler<?> helper, Class<MM> minecraftType, Class<MP> parameterType, T feature) {
        return () -> {
            MinecraftRoleType.Info<MM, MP> info = MinecraftRoleType.craft(helper, minecraftType, parameterType, feature);
            Pair<?, ?> pair = new Pair<>(minecraftType, parameterType);
            FeatureCrafter<?, ?, ?> crafter = this.crafters.get(pair);
            Class<FeatureCrafter<T, MM, MP>> clazz = Nexo.type(minecraftType);
            return clazz.cast(crafter).craft(helper, info.extender(), info.factory(), feature);
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

    public static <T extends Feature<T, U>, U extends Unit<T, ?>> @NotNull MinecraftFeatureType<T, U, ?, ?> of(Feature.Type<T, U> type) {
        Class<MinecraftFeatureType<T, U, ?, ?>> clazz = Nexo.type(MinecraftFeatureType.class);
        MinecraftFeatureType<?, ?, ?, ?> featureType = TYPES.get(type);
        if (featureType == null) {
            throw new UnsupportedOperationException("Unsupported feature type: " + type);
        }
        return clazz.cast(featureType);
    }

    public static @NotNull Collection<MinecraftFeatureType<?, ?, ?, ?>> all() {
        return TYPES.values();
    }

    private static <T, M, P> FeatureCrafter<T, M, P> crafter(FeatureCrafter<T, M, P> crafter) {
        return crafter;
    }

    @FunctionalInterface
    private interface FeatureCrafter<T, M, P> {
        @NotNull M craft(
                @NotNull NexoRegistryHandler<?> helper,
                @NotNull NexoUtils.Extender<M> extender,
                @NotNull Function<P, M> factory,
                @NotNull T feature
        );
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

