package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.dimension.NexoDimension;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
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

public class MinecraftDimension extends NexoDimension {

    @NotNull
    private final Location location;
    @NotNull
    private final Holder<LevelStem> holder;
    @Nullable
    private final NexoDimension delegate;

    public MinecraftDimension(Holder<LevelStem> holder, NexoDimension delegate) {
        this.delegate = delegate;
        this.holder = holder;
        this.location = NexoMinecraft.id(holder.unwrapKey().orElseThrow().location());
    }

    public MinecraftDimension(Holder<LevelStem> holder) {
        this(holder, null);
    }

    public @NotNull Holder<LevelStem> getHolder() {
        return holder;
    }

    @Nullable
    public NexoDimension getDelegate() {
        return delegate;
    }

    @Override
    public @NotNull Location location() {
        return location;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    public static MinecraftDimension register(ResourceLocation id, NexoDimension dimension) {
        Holder<DimensionType> type = NexoMinecraft.getHelper().registerFeature(Registries.DIMENSION_TYPE, id, () -> new DimensionType(
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
        Holder<LevelStem> holder = NexoMinecraft.getHelper().registerFeature(Registries.LEVEL_STEM, id, () -> {
            FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(
                Optional.empty(),
                NexoMinecraft.getHelper().getRegistryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.THE_VOID),
                List.of()
            );
            FlatLevelSource source = new FlatLevelSource(settings);
            return new LevelStem(type, source);
        });
        return new MinecraftDimension(holder, dimension);
    }

}
