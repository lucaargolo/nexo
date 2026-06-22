package dev.lucaargolo.nexo;

import com.google.common.collect.Maps;
import dev.lucaargolo.nexo.api.Location;
import dev.lucaargolo.nexo.api.NexoMod;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.IBlock;
import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.feature.MinecraftBlock;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class FabricNexoMinecraft extends NexoMinecraft implements ModInitializer {

    private final Map<Class<? extends IFeature>, Map<Location, IFeature>> FEATURE_REGISTRY = Maps.newHashMap();

    @Override
    public void onInitialize() {
        this.init();
    }

    @Override
    public String getPlatform() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public @Nullable NexoMod getMod(String id) {
        return this.modDiscovery.getMod(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends IFeature, I extends T> T registerFeature(Class<T> type, I feature) {
        Location location = feature.location();
        if (IBlock.class == type && feature instanceof IBlock block) {
            ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
            Holder.Reference<Block> holder = Registry.registerForHolder(
                BuiltInRegistries.BLOCK,
                blockId,
                new Block(BlockBehaviour.Properties.of())
            );
            MinecraftBlock minecraftBlock = emit(new FeatureRegisteredEvent<>(location, new MinecraftBlock(holder, block)));
            FEATURE_REGISTRY.computeIfAbsent(type, t -> Maps.newHashMap()).put(location, minecraftBlock);
            return (T) minecraftBlock;
        }
        return null;
    }

    @Override
    public @NotNull <T extends IFeature> Map<Location, IFeature> getFeatureRegistry(Class<T> type) {
        return FEATURE_REGISTRY.getOrDefault(type, Map.of());
    }
}
