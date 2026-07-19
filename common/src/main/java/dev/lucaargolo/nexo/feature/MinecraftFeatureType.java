package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.block.MinecraftBlock;
import dev.lucaargolo.nexo.feature.data.MinecraftData;
import dev.lucaargolo.nexo.feature.entity.MinecraftEntity;
import dev.lucaargolo.nexo.feature.item.MinecraftItem;
import dev.lucaargolo.nexo.feature.item.MinecraftItemCategory;
import dev.lucaargolo.nexo.feature.world.MinecraftWorld;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Bijection;
import dev.lucaargolo.nexo.util.NexoHolder;
import dev.lucaargolo.nexo.util.NexoUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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

public class MinecraftFeatureType<T extends Feature<T>, M> {

    private static final Map<Feature.Type<?>, MinecraftFeatureType<?, ?>> TYPES = new HashMap<>();

    public static final MinecraftFeatureType<DataBase<?>, DataComponentType<?>> DATA = new MinecraftFeatureType<>(
            NexoUtils.type(DataComponentType.class),
            Feature.Type.data(),
            helper -> BuiltInRegistries.DATA_COMPONENT_TYPE,
            MinecraftData.CONVERT,
            MinecraftData::lookup,
            MinecraftData::register,
            MinecraftData::index,
            Map.of(DataComponentType.class, MinecraftData::craft)
    );

    public static final MinecraftFeatureType<BlockBase, Block> BLOCK = new MinecraftFeatureType<>(
            Block.class,
            Feature.Type.BLOCK,
            helper -> BuiltInRegistries.BLOCK,
            MinecraftBlock.CONVERT,
            MinecraftBlock::lookup,
            MinecraftBlock::register,
            MinecraftBlock::index,
            Map.of(Block.class, MinecraftBlock::craft)
    );

    public static final MinecraftFeatureType<ItemBase, Item> ITEM = new MinecraftFeatureType<>(
            Item.class,
            Feature.Type.ITEM,
            helper -> BuiltInRegistries.ITEM,
            MinecraftItem.CONVERT,
            MinecraftItem::lookup,
            MinecraftItem::register,
            MinecraftItem::index,
            Map.of(Item.class, MinecraftItem::craft)
    );

    public static final MinecraftFeatureType<ItemCategoryBase, CreativeModeTab> ITEM_CATEGORY = new MinecraftFeatureType<>(
            CreativeModeTab.class,
            Feature.Type.ITEM_CATEGORY,
            helper -> BuiltInRegistries.CREATIVE_MODE_TAB,
            MinecraftItemCategory.CONVERT,
            MinecraftItemCategory::lookup,
            MinecraftItemCategory::register,
            MinecraftItemCategory::index,
            Map.of(CreativeModeTab.class, MinecraftItemCategory::craft)
    );

    public static final MinecraftFeatureType<EntityBase, EntityType<?>> ENTITY = new MinecraftFeatureType<>(
            NexoUtils.type(EntityType.class),
            Feature.Type.ENTITY,
            helper -> BuiltInRegistries.ENTITY_TYPE,
            MinecraftEntity.CONVERT,
            MinecraftEntity::lookup,
            MinecraftEntity::register,
            MinecraftEntity::index,
            Map.of(EntityType.class, MinecraftEntity::craft)
    );

    public static final MinecraftFeatureType<WorldBase, LevelStem> WORLD = new MinecraftFeatureType<>(
            LevelStem.class,
            Feature.Type.WORLD,
            helper -> helper.getRegistry().registryOrThrow(Registries.LEVEL_STEM),
            MinecraftWorld.CONVERT,
            MinecraftWorld::lookup,
            MinecraftWorld::register,
            MinecraftWorld::index,
            Map.of(DimensionType.class, MinecraftWorld::craftType, LevelStem.class, MinecraftWorld::craftStem)
    );

    private final Class<M> clazz;
    private final Feature.Type<T> type;
    private final Function<NexoRegistryHandler<?>, Registry<M>> registry;
    private final Bijection<T, NexoHolder<M>> convert;
    private final Function<Location, T> lookup;
    private final BiFunction<NexoRegistryHandler<?>, T, T> registrar;
    private final BiFunction<NexoRegistryHandler<?>, M, NexoHolder<M>> index;
    private final Map<Class<?>, BiFunction<NexoRegistryHandler<?>, T, ?>> crafters;

    private MinecraftFeatureType(
            Class<M> clazz,
            Feature.Type<T> type,
            Function<NexoRegistryHandler<?>, Registry<M>> registry,
            Bijection<T, NexoHolder<M>> convert,
            Function<Location, T> lookup,
            BiFunction<NexoRegistryHandler<?>, T, T> registrar,
            BiFunction<NexoRegistryHandler<?>, M, NexoHolder<M>> index,
            Map<Class<?>, BiFunction<NexoRegistryHandler<?>, T, ?>> crafters
    ) {
        this.clazz = clazz;
        this.type = type;
        this.registry = registry;
        this.convert = convert;
        this.lookup = lookup;
        this.registrar = registrar;
        this.index = index;
        this.crafters = crafters;
        TYPES.put(type, this);
    }

    public Registry<M> registry(NexoRegistryHandler<?> helper) {
        return this.registry.apply(helper);
    }

    public boolean isInstance(Feature<?> feature) {
        return this.type.isInstance(feature);
    }

    public @NotNull M convert(T feature) {
        return convert.forward(feature).get();
    }

    public @NotNull T convert(NexoRegistryHandler<?> helper, M feature) {
        NexoHolder<M> holder = index.apply(helper, feature);
        return convert.backward(holder);
    }

    public @Nullable T lookup(Location location) {
        return lookup.apply(location);
    }

    public @NotNull T register(NexoRegistryHandler<?> helper, Feature<?> feature) {
        return registrar.apply(helper, type.cast(feature));
    }

    public @NotNull Supplier<M> craft(NexoRegistryHandler<?> helper, T feature) {
        return craft(this.clazz, helper, feature);
    }

    public @NotNull <C> Supplier<C> craft(Class<C> type, NexoRegistryHandler<?> helper, T feature) {
        return () -> MinecraftRoleType.craft(type, feature, () -> type.cast(this.crafters.get(type).apply(helper, feature)));
    }

    public static @NotNull MinecraftFeatureType<?, ?> of(Feature.Type<?> type) {
        MinecraftFeatureType<?, ?> featureType = TYPES.get(type);
        if (featureType == null) {
            throw new UnsupportedOperationException("Unsupported feature type: " + type);
        }
        return featureType;
    }

    public static @NotNull Collection<MinecraftFeatureType<?, ?>> all() {
        return TYPES.values();
    }

}

