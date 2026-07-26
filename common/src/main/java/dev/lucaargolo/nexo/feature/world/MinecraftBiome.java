package dev.lucaargolo.nexo.feature.world;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.world.BiomeBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Bijection;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MinecraftBiome extends BiomeBase {

    private static final ConcurrentHashMap<Location, BiomeBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, Holder<Biome>> HOLDER_MAP = new ConcurrentHashMap<>();

    public static Bijection<BiomeBase, Holder<Biome>> CONVERT = new Bijection<>() {
        @Override
        public Holder<Biome> forward(BiomeBase feature) {
            return HOLDER_MAP.get(feature.location());
        }

        @Override
        public BiomeBase backward(Holder<Biome> holder) {
            return FEATURE_MAP.get(NexoMinecraft.id(holder));
        }
    };

    @NotNull
    private final Holder<Biome> holder;

    private MinecraftBiome(NexoRegistryHandler<?> helper, @NotNull Holder<Biome> holder) {
        super(NexoMinecraft.id(holder), MinecraftRoleType.uncraft(helper, Type.BIOME, holder));
        this.holder = holder;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    public static BiomeBase lookup(Location location) {
        return FEATURE_MAP.get(location);
    }

    public static BiomeBase register(NexoRegistryHandler<?> helper, BiomeBase biome) {
        BiomeBase registered = FEATURE_MAP.get(biome.location());
        if (registered != null) {
            return registered;
        }
        ResourceLocation id = NexoMinecraft.rl(biome.location());
        FEATURE_MAP.put(biome.location(), biome);
        helper.registerDynamicFeature(Registries.BIOME, id, MinecraftFeatureType.BIOME.craft(helper, biome));
        return biome;
    }

    public static BiomeBase index(NexoRegistryHandler<?> helper, Holder<Biome> holder) {
        Location location = NexoMinecraft.id(holder);
        HOLDER_MAP.put(location, holder);
        return FEATURE_MAP.computeIfAbsent(location, l -> new MinecraftBiome(helper, holder));
    }

    public static Biome craft(NexoRegistryHandler<?> helper, BiomeBase biome) {
        return new Biome.BiomeBuilder()
                .temperature(0.0f)
                .downfall(0.0f)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .fogColor(0x0)
                        .waterColor(0x0)
                        .waterFogColor(0x0)
                        .skyColor(0x0)
                        .build()
                )
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }

}
