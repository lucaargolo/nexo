package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Identifier;
import dev.lucaargolo.nexo.api.feature.Block;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.feature.MinecraftBlock;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.Nullable;

public class NexoMinecraftFabric extends NexoMinecraft implements ModInitializer {

    @Override
    public void onInitialize() {
    }

    @Override
    public @Nullable <T extends Feature> T add(Identifier id, T feature) {
        if (feature instanceof Block block) {
            net.minecraft.world.level.block.Block minecraftBlock = new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.of());
            Holder.Reference<net.minecraft.world.level.block.Block> holder = Registry.registerForHolder(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(id.namespace(), id.path()), minecraftBlock);
            cacheBlock(id, new MinecraftBlock(holder));
            return feature;
        }
        return null;
    }
}
