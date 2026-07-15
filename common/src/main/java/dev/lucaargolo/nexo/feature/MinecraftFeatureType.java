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
import dev.lucaargolo.nexo.util.NexoUtils;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MinecraftFeatureType<M> {

    private static final Map<Feature.Type<?>, MinecraftFeatureType<?>> TYPES = new HashMap<>();

    public static final MinecraftFeatureType<DataComponentType<?>> DATA = data();
    public static final MinecraftFeatureType<Block> BLOCK = create(Feature.Type.BLOCK, Block.class, MinecraftBlock::crafted, MinecraftBlock::lookup, MinecraftBlock::register, Map.of(Block.class, MinecraftBlock::craft));
    public static final MinecraftFeatureType<Item> ITEM = create(Feature.Type.ITEM, Item.class, MinecraftItem::crafted, MinecraftItem::lookup, MinecraftItem::register, Map.of(Item.class, MinecraftItem::craft));
    public static final MinecraftFeatureType<CreativeModeTab> ITEM_CATEGORY = create(Feature.Type.ITEM_CATEGORY, CreativeModeTab.class, MinecraftItemCategory::crafted, MinecraftItemCategory::lookup, MinecraftItemCategory::register, Map.of(CreativeModeTab.class, MinecraftItemCategory::craft));
    public static final MinecraftFeatureType<EntityType<?>> ENTITY = create(Feature.Type.ENTITY, NexoUtils.type(EntityType.class), MinecraftEntity::crafted, MinecraftEntity::lookup, MinecraftEntity::register, Map.of(EntityType.class, MinecraftEntity::craft));
    public static final MinecraftFeatureType<LevelStem> WORLD = create(Feature.Type.WORLD, LevelStem.class, MinecraftWorld::crafted, MinecraftWorld::lookup, MinecraftWorld::register, Map.of(DimensionType.class, MinecraftWorld::craftType, LevelStem.class, MinecraftWorld::craftStem));

    private final Feature.Type<?> type;
    private final Class<M> clazz;
    private final Function<Feature<?>, M> crafted;
    private final BiFunction<NexoRegistryHandler<?>, Location, Feature<?>> lookup;
    private final BiFunction<NexoRegistryHandler<?>, Feature<?>, Feature<?>> registrar;
    private final Map<Class<?>, BiFunction<NexoRegistryHandler<?>, Feature<?>, ?>> crafters;

    private MinecraftFeatureType(
            Feature.Type<?> type,
            Class<M> clazz,
            Function<Feature<?>, M> crafted,
            BiFunction<NexoRegistryHandler<?>, Location, Feature<?>> lookup,
            BiFunction<NexoRegistryHandler<?>, Feature<?>, Feature<?>> registrar,
            Map<Class<?>, BiFunction<NexoRegistryHandler<?>, Feature<?>, ?>> crafters
    ) {
        this.type = type;
        this.clazz = clazz;
        this.crafted = crafted;
        this.lookup = lookup;
        this.registrar = registrar;
        this.crafters = crafters;
    }

    public boolean isInstance(Feature<?> feature) {
        return this.type.isInstance(feature);
    }

    public @NotNull M crafted(Feature<?> feature) {
        return crafted.apply(type.cast(feature));
    }

    public @Nullable Feature<?> lookup(NexoRegistryHandler<?> helper, Location id) {
        return lookup.apply(helper, id);
    }

    public @NotNull Feature<?> register(NexoRegistryHandler<?> helper, Feature<?> feature) {
        return registrar.apply(helper, type.cast(feature));
    }

    public @NotNull Supplier<M> craft(NexoRegistryHandler<?> helper, Feature<?> feature) {
        return craft(this.clazz, helper, feature);
    }

    public @NotNull <T> Supplier<T> craft(Class<T> type, NexoRegistryHandler<?> helper, Feature<?> feature) {
        return () -> type.cast(this.crafters.get(type).apply(helper, feature));
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
            Class<M> clazz,
            Function<T, M> crafted,
            BiFunction<NexoRegistryHandler<?>, Location, T> lookup,
            BiFunction<NexoRegistryHandler<?>, T, T> registrar,
            Map<Class<?>, BiFunction<NexoRegistryHandler<?>, T, ?>> crafters
    ) {
        Map<Class<?>, BiFunction<NexoRegistryHandler<?>, Feature<?>, ?>> typedCrafters = new HashMap<>();
        crafters.forEach((craftType, crafter) -> typedCrafters.put(
                craftType,
                (helper, feature) -> crafter.apply(helper, type.cast(feature))
        ));
        MinecraftFeatureType<M> mft = new MinecraftFeatureType<>(
                type,
                clazz,
                (feature) -> crafted.apply(type.cast(feature)),
                lookup::apply,
                (helper, feature) -> registrar.apply(helper, type.cast(feature)),
                typedCrafters
            );
        TYPES.put(type, mft);
        return mft;
    }

    private static MinecraftFeatureType<DataComponentType<?>> data() {
        MinecraftFeatureType<DataComponentType<?>> mft = new MinecraftFeatureType<>(
                Feature.Type.DATA,
                NexoUtils.type(DataComponentType.class),
                (feature) -> MinecraftData.crafted((DataBase<?>) feature),
                MinecraftData::lookup,
                (helper, feature) -> MinecraftData.register(helper, (DataBase<?>) feature),
                Map.of(DataComponentType.class, (helper, feature) -> MinecraftData.craft(helper, (DataBase<?>) feature))
        );
        TYPES.put(Feature.Type.DATA, mft);
        return mft;
    }

}

