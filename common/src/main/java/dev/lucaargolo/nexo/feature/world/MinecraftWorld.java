package dev.lucaargolo.nexo.feature.world;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Bijection;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

public class MinecraftWorld extends WorldBase {

    private static final ConcurrentHashMap<Location, WorldBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, Holder<LevelStem>> HOLDER_MAP = new ConcurrentHashMap<>();

    public static Bijection<WorldBase, Holder<LevelStem>> CONVERT = new Bijection<>() {
        @Override
        public Holder<LevelStem> forward(WorldBase feature) {
            return HOLDER_MAP.get(feature.location());
        }

        @Override
        public WorldBase backward(Holder<LevelStem> holder) {
            return FEATURE_MAP.get(NexoMinecraft.id(holder));
        }
    };

    @NotNull
    private final Holder<LevelStem> holder;

    private MinecraftWorld(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Holder<LevelStem> holder) {
        super(NexoMinecraft.id(holder), MinecraftRoleType.uncraft(nexo, Type.WORLD, holder));
        this.holder = holder;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    public static WorldBase lookup(Location location) {
        return FEATURE_MAP.get(location);
    }

    public static WorldBase register(NexoMinecraft<?, ?, ?, ?> nexo, WorldBase world) {
        WorldBase registered = FEATURE_MAP.get(world.location());
        if (registered != null) {
            return registered;
        }
        ResourceLocation id = NexoMinecraft.rl(world.location());
        FEATURE_MAP.put(world.location(), world);
        nexo.getRegistryHandler().registerDynamicFeature(Registries.DIMENSION_TYPE, id, MinecraftFeatureType.WORLD.craft(nexo, DimensionType.class, world));
        nexo.getRegistryHandler().registerDynamicFeature(Registries.LEVEL_STEM, id, MinecraftFeatureType.WORLD.craft(nexo, world));
        return world;
    }

    public static WorldBase index(NexoMinecraft<?, ?, ?, ?> nexo, Holder<LevelStem> holder) {
        Location location = NexoMinecraft.id(holder);
        HOLDER_MAP.put(location, holder);
        return FEATURE_MAP.computeIfAbsent(location, l -> new MinecraftWorld(nexo, holder));
    }

    public static DimensionType craftType(NexoMinecraft<?, ?, ?, ?> nexo, WorldBase world) {
        return new DimensionType(
                OptionalLong.empty(),
                true,
                false,
                false,
                true,
                1.0,
                true,
                false,
                -64,
                384,
                384,
                BlockTags.INFINIBURN_OVERWORLD,
                BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                0.0F,
                new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)
        );
    }

    public static LevelStem craftStem(NexoMinecraft<?, ?, ?, ?> nexo, WorldBase world) {
        ResourceLocation id = NexoMinecraft.rl(world.location());
        ResourceKey<DimensionType> key = ResourceKey.create(Registries.DIMENSION_TYPE, id);
        Holder<DimensionType> type = nexo.getRegistryHandler().getDynamicFeature(key);
        Holder<Biome> biomeHolder = nexo.getRegistryHandler().getDynamicFeature(Biomes.THE_VOID);
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.empty(), biomeHolder, List.of());
        FlatLevelSource source = new FlatLevelSource(settings);
        return new LevelStem(type, source);
    }

}
