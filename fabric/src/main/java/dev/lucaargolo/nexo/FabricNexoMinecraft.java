package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Location;
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
import org.jetbrains.annotations.Nullable;

public class FabricNexoMinecraft extends NexoMinecraft implements ModInitializer {

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
    @SuppressWarnings("unchecked")
    public @Nullable <T extends IFeature, I extends T> T registerFeature(Class<T> type, Location location, I feature) {
        if (type.isAssignableFrom(IBlock.class) && feature instanceof IBlock block) {
            Holder.Reference<Block> holder = Registry.registerForHolder(
                BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path()),
                new Block(BlockBehaviour.Properties.of())
            );
            MinecraftBlock minecraftBlock = new MinecraftBlock(holder);
            cacheBlock(location, minecraftBlock);
            return (T) minecraftBlock;
        }
        return null;
    }
}
