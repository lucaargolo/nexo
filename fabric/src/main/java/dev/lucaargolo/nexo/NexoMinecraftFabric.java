package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Identifier;
import dev.lucaargolo.nexo.api.feature.Block;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.feature.MinecraftBlock;
import dev.lucaargolo.nexo.fabric.NexoModContainer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class NexoMinecraftFabric extends NexoMinecraft implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("Nexo|FabricRegistration");

    @Override
    public void onInitialize() {
        this.init();

        // Use reflection to directly add discovered Nexo mods into
        // FabricLoaderImpl's internal modMap and mods list, so they appear
        // in ModMenu and are visible via isModLoaded().
        try {
            FabricLoaderImpl impl = (FabricLoaderImpl) (Object) FabricLoader.getInstance();
            
            Field modsField = FabricLoaderImpl.class.getDeclaredField("mods");
            modsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> mods = (List<Object>) modsField.get(impl);
            
            Field modMapField = FabricLoaderImpl.class.getDeclaredField("modMap");
            modMapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> modMap = (Map<String, Object>) modMapField.get(impl);
            
            for (NexoMod mod : this.getModDiscovery().getMods()) {
                NexoModContainer container = new NexoModContainer(mod);
                mods.add(container);
                modMap.put(container.getMetadata().getId(), container);
                LOGGER.info("Registered Nexo mod '{}' in FabricLoader internals", container.getMetadata().getId());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to register Nexo mods in FabricLoader: {}", e.getMessage());
        }
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
