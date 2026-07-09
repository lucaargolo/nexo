package dev.lucaargolo.nexo.feature.world;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.world.NexoWorld;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeature;
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

public class MinecraftWorld extends NexoWorld implements MinecraftFeature<NexoWorld, LevelStem> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final Location location;
    @NotNull
    private final LazyHolder<LevelStem> holder;
    @Nullable
    private final NexoWorld delegate;

    public MinecraftWorld(@NotNull NexoMinecraft nexo, @NotNull LazyHolder<LevelStem> holder, @Nullable NexoWorld delegate) {
        this.nexo = nexo;
        this.delegate = delegate;
        this.holder = holder;
        this.location = NexoMinecraft.id(holder.key());
    }

    public MinecraftWorld(@NotNull NexoMinecraft nexo, @NotNull Holder<LevelStem> holder) {
        this(nexo, new LazyHolder<>(nexo, holder), null);
    }

    @Override
    public @NotNull NexoMinecraft nexo() {
        return this.nexo;
    }

    @Override
    public @NotNull Holder<LevelStem> holder() {
        return this.holder.get();
    }

    @Override
    public @Nullable NexoWorld delegate() {
        return this.delegate;
    }

    @Override
    public @NotNull Location location() {
        return this.location;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.get().tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    public static MinecraftWorld register(NexoRegistryHandler<?> helper, ResourceLocation id, NexoWorld dimension) {
        LazyHolder<DimensionType> type = helper.registerDynamicFeature(Registries.DIMENSION_TYPE, id, () -> new DimensionType(
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
        LazyHolder<LevelStem> holder = helper.registerDynamicFeature(Registries.LEVEL_STEM, id, () -> {
            RegistryAccess access = helper.getRegistry();
            Registry<Biome> biomeRegistry = access.registryOrThrow(Registries.BIOME);
            Holder<Biome> biomeHolder = biomeRegistry.getHolderOrThrow(Biomes.THE_VOID);
            FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.empty(), biomeHolder, List.of());
            FlatLevelSource source = new FlatLevelSource(settings);
            return new LevelStem(type.get(), source);
        });
        return new MinecraftWorld(helper.nexo(), holder, dimension);
    }

}
