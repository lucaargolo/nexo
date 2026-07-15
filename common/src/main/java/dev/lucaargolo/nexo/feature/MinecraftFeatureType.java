package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.block.MinecraftBlock;
import dev.lucaargolo.nexo.feature.data.MinecraftData;
import dev.lucaargolo.nexo.feature.entity.MinecraftEntity;
import dev.lucaargolo.nexo.feature.item.MinecraftItem;
import dev.lucaargolo.nexo.feature.item.MinecraftItemCategory;
import dev.lucaargolo.nexo.feature.world.MinecraftWorld;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.LevelStem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MinecraftFeatureType<M> {

    private static final Map<Feature.Type<?>, MinecraftFeatureType<?>> TYPES = new HashMap<>();

    public static final MinecraftFeatureType<DataComponentType<?>> DATA = data();
    public static final MinecraftFeatureType<Block> BLOCK = create(Feature.Type.BLOCK, MinecraftBlock::crafted, MinecraftBlock::lookup, MinecraftBlock::register);
    public static final MinecraftFeatureType<EntityType<?>> ENTITY = create(Feature.Type.ENTITY, MinecraftEntity::crafted, MinecraftEntity::lookup, MinecraftEntity::register);
    public static final MinecraftFeatureType<Item> ITEM = create(Feature.Type.ITEM, MinecraftItem::crafted, MinecraftItem::lookup, MinecraftItem::register);
    public static final MinecraftFeatureType<CreativeModeTab> ITEM_CATEGORY = create(Feature.Type.ITEM_CATEGORY, MinecraftItemCategory::crafted, MinecraftItemCategory::lookup, MinecraftItemCategory::register);
    public static final MinecraftFeatureType<LevelStem> WORLD = create(Feature.Type.WORLD, MinecraftWorld::crafted, MinecraftWorld::lookup, MinecraftWorld::register);

    private final Feature.Type<?> type;
    private final Function<Feature<?>, M> crafted;
    private final BiFunction<NexoRegistryHandler<?>, Location, Feature<?>> lookup;
    private final TriFunction<NexoRegistryHandler<?>, ResourceLocation, Feature<?>, Feature<?>> registrar;

    private MinecraftFeatureType(
            Feature.Type<?> type,
            Function<Feature<?>, M> crafted,
            BiFunction<NexoRegistryHandler<?>, Location, Feature<?>> lookup,
            TriFunction<NexoRegistryHandler<?>, ResourceLocation, Feature<?>, Feature<?>> registrar

    ) {
        this.type = type;
        this.crafted = crafted;
        this.lookup = lookup;
        this.registrar = registrar;
    }

    public boolean isInstance(Feature<?> feature) {
        return this.type.isInstance(feature);
    }

    public @Nullable Feature<?> lookup(NexoRegistryHandler<?> helper, Location id) {
        return lookup.apply(helper, id);
    }

    public @NotNull Feature<?> register(NexoRegistryHandler<?> helper, ResourceLocation id, Feature<?> feature) {
        return registrar.apply(helper, id, type.cast(feature));
    }

    public @NotNull M craft(Feature<?> feature) {
        return crafted.apply(type.cast(feature));
    }

    public static @NotNull MinecraftFeatureType<?> of(Feature.Type<?> type) {
        MinecraftFeatureType<?> featureType = TYPES.get(type);
        if (featureType == null) {
            throw new UnsupportedOperationException("Unsupported feature type: " + type);
        }
        return featureType;
    }

    private static <T extends Feature<T>, M> MinecraftFeatureType<M> create(
            Feature.Type<T> type,
            Function<T, M> crafted,
            BiFunction<NexoRegistryHandler<?>, Location, T> lookup,
            TriFunction<NexoRegistryHandler<?>, ResourceLocation, T, T> registrar
    ) {
        MinecraftFeatureType<M> mft = new MinecraftFeatureType<>(
                type,
                (feature) -> crafted.apply(type.cast(feature)),
                lookup::apply,
                (helper, id, feature) -> registrar.apply(helper, id, type.cast(feature))
        );
        TYPES.put(type, mft);
        return mft;
    }

    private static MinecraftFeatureType<DataComponentType<?>> data() {
        MinecraftFeatureType<DataComponentType<?>> mft = new MinecraftFeatureType<>(
                Feature.Type.DATA,
                (feature) -> MinecraftData.crafted((DataBase<?>) feature),
                MinecraftData::lookup,
                (helper, id, feature) -> MinecraftData.register(helper, id, (DataBase<?>) feature)
        );
        TYPES.put(Feature.Type.DATA, mft);
        return mft;
    }

    @FunctionalInterface
    private interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

}

