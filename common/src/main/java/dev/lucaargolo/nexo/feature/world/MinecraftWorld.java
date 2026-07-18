package dev.lucaargolo.nexo.feature.world;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Bijection;
import dev.lucaargolo.nexo.util.NexoHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
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
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

public class MinecraftWorld extends WorldBase {

    private static final ConcurrentHashMap<Location, WorldBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, NexoHolder<LevelStem>> HOLDER_MAP = new ConcurrentHashMap<>();

    public static Bijection<WorldBase, NexoHolder<LevelStem>> CONVERT = new Bijection<>() {
        @Override
        public NexoHolder<LevelStem> forward(WorldBase feature) {
            return HOLDER_MAP.get(feature.location());
        }

        @Override
        public WorldBase backward(NexoHolder<LevelStem> holder) {
            return FEATURE_MAP.get(holder.location());
        }
    };

    @NotNull
    private final NexoHolder<LevelStem> holder;

    private MinecraftWorld(NexoRegistryHandler<?> helper, @NotNull NexoHolder<LevelStem> holder) {
        super(NexoMinecraft.id(holder.key()), MinecraftRoleType.uncraft(helper, Type.WORLD, holder));
        this.holder = holder;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    public static WorldBase lookup(Location location) {
        return FEATURE_MAP.get(location);
    }

    public static WorldBase register(NexoRegistryHandler<?> helper, WorldBase world) {
        WorldBase registered = FEATURE_MAP.get(world.location());
        if (registered != null) {
            return registered;
        }
        ResourceLocation id = NexoMinecraft.rl(world.location());
        helper.registerDynamicFeature(Registries.DIMENSION_TYPE, id, MinecraftFeatureType.WORLD.craft(DimensionType.class, helper, world), DimensionType.class);
        NexoHolder<LevelStem> holder = helper.registerDynamicFeature(Registries.LEVEL_STEM, id, MinecraftFeatureType.WORLD.craft(helper, world), LevelStem.class);
        FEATURE_MAP.put(world.location(), world);
        HOLDER_MAP.put(world.location(), holder);
        return world;
    }

    public static NexoHolder<LevelStem> index(NexoRegistryHandler<?> helper, LevelStem world) {
        RegistryAccess access = helper.getRegistry();
        Registry<LevelStem> registry = access.registryOrThrow(Registries.LEVEL_STEM);
        ResourceLocation location = Objects.requireNonNull(registry.getKey(world));
        Location featureLocation = NexoMinecraft.id(location);
        NexoHolder<LevelStem> indexed = HOLDER_MAP.get(featureLocation);
        if (indexed != null) {
            return indexed;
        }
        Holder<LevelStem> h = registry.getHolder(location).orElseThrow();
        NexoHolder<LevelStem> holder = new NexoHolder<>(helper.nexo(), h, LevelStem.class);
        FEATURE_MAP.putIfAbsent(featureLocation, new MinecraftWorld(helper, holder));
        HOLDER_MAP.put(featureLocation, holder);
        return holder;
    }

    public static DimensionType craftType(NexoRegistryHandler<?> helper, WorldBase world) {
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

    public static LevelStem craftStem(NexoRegistryHandler<?> helper, WorldBase world) {
        ResourceLocation id = NexoMinecraft.rl(world.location());
        ResourceKey<DimensionType> key = ResourceKey.create(Registries.DIMENSION_TYPE, id);
        NexoHolder<DimensionType> type = helper.getDynamicFeature(key);
        RegistryAccess access = helper.getRegistry();
        Registry<Biome> biomeRegistry = access.registryOrThrow(Registries.BIOME);
        Holder<Biome> biomeHolder = biomeRegistry.getHolderOrThrow(Biomes.THE_VOID);
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.empty(), biomeHolder, List.of());
        FlatLevelSource source = new FlatLevelSource(settings);
        return new LevelStem(type.holder(), source);
    }

}
