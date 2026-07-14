package dev.lucaargolo.nexo.feature.world;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.util.NexoHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
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
    private static final ConcurrentHashMap<Location, NexoHolder<LevelStem, LevelStem>> HOLDER_MAP = new ConcurrentHashMap<>();

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final NexoHolder<LevelStem, LevelStem> holder;

    private MinecraftWorld(@NotNull NexoMinecraft nexo, @NotNull NexoHolder<LevelStem, LevelStem> holder) {
        super(NexoMinecraft.id(holder.key()));
        this.nexo = nexo;
        this.holder = holder;
    }

    private MinecraftWorld(@NotNull NexoMinecraft nexo, @NotNull Holder<LevelStem> holder) {
        this(nexo, new NexoHolder<>(nexo, holder, LevelStem.class));
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    public static WorldBase lookup(NexoRegistryHandler<?> helper, Location location) {
        return FEATURE_MAP.computeIfAbsent(location, l -> {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
            MinecraftWorld world = helper.getRegistry().registry(Registries.LEVEL_STEM).flatMap(r -> r.getHolder(id)).map(h -> new MinecraftWorld(helper.nexo(), h)).orElse(null);
            if (world != null) HOLDER_MAP.put(location, world.holder);
            return world;
        });
   }

    public static WorldBase register(NexoRegistryHandler<?> helper, ResourceLocation id, WorldBase world) {
        NexoHolder<DimensionType, DimensionType> type = helper.registerDynamicFeature(Registries.DIMENSION_TYPE, id, () -> new DimensionType(
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
        ), DimensionType.class);
        NexoHolder<LevelStem, LevelStem> holder = helper.registerDynamicFeature(Registries.LEVEL_STEM, id, () -> {
            RegistryAccess access = helper.getRegistry();
            Registry<Biome> biomeRegistry = access.registryOrThrow(Registries.BIOME);
            Holder<Biome> biomeHolder = biomeRegistry.getHolderOrThrow(Biomes.THE_VOID);
            FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.empty(), biomeHolder, List.of());
            FlatLevelSource source = new FlatLevelSource(settings);
            return new LevelStem(type.holder(), source);
        }, LevelStem.class);
        FEATURE_MAP.put(world.location(), world);
        HOLDER_MAP.put(world.location(), holder);
        return world;
    }

    public static LevelStem craft(WorldBase world) {
        return Objects.requireNonNull(HOLDER_MAP.get(world.location()).get());
    }

}
