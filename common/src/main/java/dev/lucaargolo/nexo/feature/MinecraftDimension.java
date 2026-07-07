package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.dimension.NexoDimension;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.util.LazyHolder;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public class MinecraftDimension extends NexoDimension implements MinecraftFeature<NexoDimension, LevelStem> {

    @NotNull
    private final Location location;
    @NotNull
    private final LazyHolder<LevelStem> holder;
    @Nullable
    private final NexoDimension delegate;

    public MinecraftDimension(LazyHolder<LevelStem> holder, NexoDimension delegate) {
        this.delegate = delegate;
        this.holder = holder;
        this.location = NexoMinecraft.id(holder.key());
    }

    public MinecraftDimension(Holder<LevelStem> holder) {
        this(new LazyHolder<>(holder), null);
    }

    @Override
    public @NotNull Holder<LevelStem> holder() {
        return holder.get();
    }

    @Override
    public @Nullable NexoDimension delegate() {
        return delegate;
    }

    @Override
    public @NotNull Location location() {
        return location;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return holder.get().tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    public static MinecraftDimension register(ResourceLocation id, NexoDimension dimension) {
        LazyHolder<DimensionType> type = NexoMinecraft.getHelper().registerDynamicFeature(Registries.DIMENSION_TYPE, id, () -> new DimensionType(
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
        ));
        LazyHolder<LevelStem> holder = NexoMinecraft.getHelper().registerDynamicFeature(Registries.LEVEL_STEM, id, () -> {
            RegistryAccess access = NexoMinecraft.getHelper().getRegistry();
            Registry<Biome> biomeRegistry = access.registryOrThrow(Registries.BIOME);
            Holder<Biome> biomeHolder = biomeRegistry.getHolderOrThrow(Biomes.THE_VOID);
            FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.empty(), biomeHolder, List.of());
            FlatLevelSource source = new FlatLevelSource(settings);
            return new LevelStem(type.get(), source);
        });
        return new MinecraftDimension(holder, dimension);
    }

}
