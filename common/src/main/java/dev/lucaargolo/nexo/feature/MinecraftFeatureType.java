package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.block.MinecraftBlock;
import dev.lucaargolo.nexo.feature.data.MinecraftData;
import dev.lucaargolo.nexo.feature.item.MinecraftItem;
import dev.lucaargolo.nexo.feature.item.MinecraftItemCategory;
import dev.lucaargolo.nexo.feature.world.MinecraftWorld;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.LevelStem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MinecraftFeatureType<M> {

    public static final MinecraftFeatureType<Block> BLOCK = create(BlockBase.class, MinecraftBlock::lookup, MinecraftBlock::register, MinecraftBlock::craft);
    public static final MinecraftFeatureType<DataComponentType<?>> DATA = new MinecraftFeatureType<>(DataBase.class, MinecraftData::lookup, MinecraftData::registerFeature, MinecraftData::craftFeature);
    public static final MinecraftFeatureType<Item> ITEM = create(ItemBase.class, MinecraftItem::lookup, MinecraftItem::register, MinecraftItem::craft);
    public static final MinecraftFeatureType<CreativeModeTab> ITEM_CATEGORY = create(ItemCategoryBase.class, MinecraftItemCategory::lookup, MinecraftItemCategory::register, MinecraftItemCategory::craft);
    public static final MinecraftFeatureType<LevelStem> WORLD = create(WorldBase.class, MinecraftWorld::lookup, MinecraftWorld::register, MinecraftWorld::craft);
    private static final Map<Feature.Type<?>, MinecraftFeatureType<?>> TYPES = Map.of(
            Feature.Type.BLOCK, BLOCK,
            Feature.Type.DATA, DATA,
            Feature.Type.ITEM, ITEM,
            Feature.Type.ITEM_CATEGORY, ITEM_CATEGORY,
            Feature.Type.WORLD, WORLD
    );

    private final Class<?> featureClass;
    private final BiFunction<NexoRegistryHandler<?>, Location, Feature<?>> lookup;
    private final TriFunction<NexoRegistryHandler<?>, ResourceLocation, Feature<?>, Feature<?>> registrar;
    private final Function<Feature<?>, M> craftar;

    private MinecraftFeatureType(
            Class<?> featureClass,
            BiFunction<NexoRegistryHandler<?>, Location, Feature<?>> lookup,
            TriFunction<NexoRegistryHandler<?>, ResourceLocation, Feature<?>, Feature<?>> registrar,
            Function<Feature<?>, M> craftar
    ) {
        this.featureClass = featureClass;
        this.lookup = lookup;
        this.registrar = registrar;
        this.craftar = craftar;
    }

    @Nullable
    public Feature<?> lookup(NexoRegistryHandler<?> helper, Location id) {
        return lookup.apply(helper, id);
    }

    @NotNull
    public Feature<?> register(NexoRegistryHandler<?> helper, ResourceLocation id, Feature<?> feature) {
        return registrar.apply(helper, id, checked(feature));
    }

    @NotNull
    public M craft(Feature<?> feature) {
        return craftar.apply(checked(feature));
    }

    private Feature<?> checked(Feature<?> feature) {
        if (!featureClass.isInstance(feature)) {
            throw new ClassCastException("Expected " + featureClass.getName() + ", got " + feature.getClass().getName());
        }
        return feature;
    }

    private static <T extends Feature<?>, M> MinecraftFeatureType<M> create(
            Class<T> featureClass,
            BiFunction<NexoRegistryHandler<?>, Location, T> lookup,
            TriFunction<NexoRegistryHandler<?>, ResourceLocation, T, T> registrar,
            Function<T, M> craftar
    ) {
        return new MinecraftFeatureType<>(
                featureClass,
                lookup::apply,
                (helper, id, feature) -> registrar.apply(helper, id, featureClass.cast(feature)),
                feature -> craftar.apply(featureClass.cast(feature))
        );
    }

    @NotNull
    public static MinecraftFeatureType<?> of(Feature.Type<?> type) {
        MinecraftFeatureType<?> featureType = TYPES.get(type);
        if (featureType == null) {
            throw new UnsupportedOperationException("Unsupported feature type: " + type);
        }
        return featureType;
    }

    public boolean isInstance(Feature<?> feature) {
        return this.featureClass.isInstance(feature);
    }

    @FunctionalInterface
    private interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

}


