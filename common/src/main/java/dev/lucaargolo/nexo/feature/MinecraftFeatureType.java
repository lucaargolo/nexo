package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
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

public class MinecraftFeatureType<T extends Feature<T>, M> {

    public static final MinecraftFeatureType<BlockBase, Block> BLOCK = new MinecraftFeatureType<>(Feature.Type.BLOCK, MinecraftBlock::lookup, MinecraftBlock::register, MinecraftBlock::craft);
    public static final MinecraftFeatureType<?, DataComponentType<?>> DATA = new MinecraftFeatureType<>(Feature.Type.data(), MinecraftData::lookup, MinecraftData::register, MinecraftData::craft);
    public static final MinecraftFeatureType<ItemBase, Item> ITEM = new MinecraftFeatureType<>(Feature.Type.ITEM, MinecraftItem::lookup, MinecraftItem::register, MinecraftItem::craft);
    public static final MinecraftFeatureType<ItemCategoryBase, CreativeModeTab> ITEM_CATEGORY = new MinecraftFeatureType<>(Feature.Type.ITEM_CATEGORY, MinecraftItemCategory::lookup, MinecraftItemCategory::register, MinecraftItemCategory::craft);
    public static final MinecraftFeatureType<WorldBase, LevelStem> WORLD = new MinecraftFeatureType<>(Feature.Type.WORLD, MinecraftWorld::lookup, MinecraftWorld::register, MinecraftWorld::craft);

    private final Feature.Type<T> type;
    private final FeatureLookup<T> lookup;
    private final FeatureRegistrar<T> registrar;
    private final FeatureCraftar<T, M> craftar;

    private MinecraftFeatureType(Feature.Type<T> type, FeatureLookup<T> lookup, FeatureRegistrar<T> registrar, FeatureCraftar<T, M> craftar) {
        this.type = type;
        this.lookup = lookup;
        this.registrar = registrar;
        this.craftar = craftar;
    }

    @Nullable
    public T lookup(NexoRegistryHandler<?> helper, Location id) {
        return lookup.lookup(helper, id);
    }

    @NotNull
    public T register(NexoRegistryHandler<?> helper, ResourceLocation id, Feature<?> feature) {
        return registrar.register(helper, id, type.cast(feature));
    }

    @NotNull
    public M craft(Feature<?> feature) {
        return craftar.craft(type.cast(feature));
    }

    @NotNull
    public static MinecraftFeatureType<?, ?> of(Feature.Type<?> type) {
        if (type == Feature.Type.BLOCK) {
            return BLOCK;
        } else if(type == Feature.Type.DATA) {
            return DATA;
        } else if (type == Feature.Type.ITEM) {
            return ITEM;
        } else if (type == Feature.Type.ITEM_CATEGORY) {
            return ITEM_CATEGORY;
        } else if (type == Feature.Type.WORLD) {
            return WORLD;
        }
        throw new UnsupportedOperationException("Unsupported feature type: " + type);
    }

    public boolean isInstance(Feature<?> feature) {
        return this.type.isInstance(feature);
    }

    @FunctionalInterface
    private interface FeatureRegistrar<T extends Feature<T>> {
        T register(NexoRegistryHandler<?> helper, ResourceLocation id, T feature);
    }

    @FunctionalInterface
    private interface FeatureLookup<T extends Feature<T>> {
        T lookup(NexoRegistryHandler<?> helper, Location location);
    }

    @FunctionalInterface
    private interface FeatureCraftar<T extends Feature<T>, M> {
        M craft(T feature);
    }

}


