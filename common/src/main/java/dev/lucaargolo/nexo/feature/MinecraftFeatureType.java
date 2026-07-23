package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
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
            Map.of(DataComponentType.class, MinecraftData::craft)
    );

    public static final MinecraftFeatureType<BlockBase, BlockUnit<?>, Block> BLOCK = new MinecraftFeatureType<>(
            Block.class,
            Feature.Type.BLOCK,
            Registries.BLOCK,
            MinecraftBlock::register,
            MinecraftBlock::index,
            MinecraftBlock::lookup,
            MinecraftBlock.CONVERT,
            Map.of(Block.class, MinecraftBlock::craft),
            (nexo, helper, feature, block) -> new MinecraftBlockUnit<>(nexo, feature, feature.role(), block.defaultBlockState())
    );

    public static final MinecraftFeatureType<ItemBase, ItemUnit<?>, Item> ITEM = new MinecraftFeatureType<>(
            Item.class,
            Feature.Type.ITEM,
            Registries.ITEM,
            MinecraftItem::register,
            MinecraftItem::index,
            MinecraftItem::lookup,
            MinecraftItem.CONVERT,
            Map.of(Item.class, MinecraftItem::craft),
            (nexo, helper, feature, item) -> new MinecraftItemUnit<>(nexo, feature, feature.role(), item.getDefaultInstance())
    );

    public static final MinecraftFeatureType<ItemCategoryBase, ItemCategoryUnit<?>, CreativeModeTab> ITEM_CATEGORY = new MinecraftFeatureType<>(
            CreativeModeTab.class,
            Feature.Type.ITEM_CATEGORY,
            Registries.CREATIVE_MODE_TAB,
            MinecraftItemCategory::register,
            MinecraftItemCategory::index,
            MinecraftItemCategory::lookup,
            MinecraftItemCategory.CONVERT,
            Map.of(CreativeModeTab.class, MinecraftItemCategory::craft),
            (nexo, helper, feature, tab) -> NexoUtils.loadPlatformClass(nexo, MinecraftItemCategoryUnit.class, helper, feature, feature.role(), tab)
    );

    public static final MinecraftFeatureType<EntityBase, EntityUnit<?>, EntityType<?>> ENTITY = new MinecraftFeatureType<>(
            Nexo.type(EntityType.class),
            Feature.Type.ENTITY,
            Registries.ENTITY_TYPE,
            MinecraftEntity::register,
            MinecraftEntity::index,
            MinecraftEntity::lookup,
            MinecraftEntity.CONVERT,
            Map.of(EntityType.class, MinecraftEntity::craft)
    );

    public static final MinecraftFeatureType<WorldBase, WorldUnit<?>, LevelStem> WORLD = new MinecraftFeatureType<>(
            LevelStem.class,
            Feature.Type.WORLD,
            Registries.LEVEL_STEM,
            MinecraftWorld::register,
            MinecraftWorld::index,
            MinecraftWorld::lookup,
            MinecraftWorld.CONVERT,
            Map.of(DimensionType.class, MinecraftWorld::craftType, LevelStem.class, MinecraftWorld::craftStem)
    );

    public static final MinecraftFeatureType<BiomeBase, Unit<BiomeBase, ?>, Biome> BIOME = new MinecraftFeatureType<>(
            Biome.class,
            Feature.Type.BIOME,
            Registries.BIOME,
            MinecraftBiome::register,
            MinecraftBiome::index,
            MinecraftBiome::lookup,
            MinecraftBiome.CONVERT,
            Map.of(DimensionType.class, MinecraftBiome::craft)
    );

    private final Class<M> clazz;
    private final Feature.Type<T, U> type;
    private final ResourceKey<? extends Registry<M>> registry;
    private final BiFunction<NexoRegistryHandler<?>, T, T> registrar;
    private final BiFunction<NexoRegistryHandler<?>, Holder<M>, T> index;
    private final Function<Location, T> lookup;
    private final Bijection<T, Holder<M>> convert;
    private final Map<Class<?>, BiFunction<NexoRegistryHandler<?>, T, ?>> crafters;
    private final @Nullable UnitFactory<T, U, M> unitFactory;

    private MinecraftFeatureType(
            Class<M> clazz,
            Feature.Type<T, U> type,
            ResourceKey<? extends Registry<M>> registry,
            BiFunction<NexoRegistryHandler<?>, T, T> registrar,
            BiFunction<NexoRegistryHandler<?>, Holder<M>, T> index,
            Function<Location, T> lookup,
            Bijection<T, Holder<M>> convert,
            Map<Class<?>, BiFunction<NexoRegistryHandler<?>, T, ?>> crafters
    ) {
        this(clazz, type, registry, registrar, index, lookup, convert, crafters, null);
    }

    private MinecraftFeatureType(
            Class<M> clazz,
            Feature.Type<T, U> type,
            ResourceKey<? extends Registry<M>> registry,
            BiFunction<NexoRegistryHandler<?>, T, T> registrar,
            BiFunction<NexoRegistryHandler<?>, Holder<M>, T> index,
            Function<Location, T> lookup,
            Bijection<T, Holder<M>> convert,
            Map<Class<?>, BiFunction<NexoRegistryHandler<?>, T, ?>> crafters,
            @Nullable UnitFactory<T, U, M> unitFactory
    ) {
        this.clazz = clazz;
        this.type = type;
        this.registry = registry;
        this.registrar = registrar;
        this.index = index;
        this.lookup = lookup;
        this.convert = convert;
        this.crafters = crafters;
        this.unitFactory = unitFactory;
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
        return craft(this.clazz, helper, feature);
    }

    public @NotNull <C> Supplier<C> craft(Class<C> type, NexoRegistryHandler<?> helper, T feature) {
        return () -> MinecraftRoleType.craft(type, feature, () -> type.cast(this.crafters.get(type).apply(helper, feature)));
    }

    public @Nullable U unit(
            @NotNull NexoMinecraft nexo,
            @NotNull NexoRegistryHandler<?> helper,
            @NotNull Feature<?, ?> feature
    ) {
        if (unitFactory != null) {
            T value = type.cast(feature);
            return unitFactory.create(nexo, helper, value, convert(value));
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

    @FunctionalInterface
    private interface UnitFactory<T extends Feature<T, U>, U extends Unit<T, ?>, M> {
        @NotNull U create(
                @NotNull NexoMinecraft nexo,
                @NotNull NexoRegistryHandler<?> helper,
                @NotNull T feature,
                @NotNull M minecraft
        );
    }

}

