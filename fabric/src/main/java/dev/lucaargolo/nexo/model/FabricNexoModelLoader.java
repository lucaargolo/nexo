package dev.lucaargolo.nexo.model;

import dev.lucaargolo.nexo.NexoAtlas;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Location;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.NexoMod;
import dev.lucaargolo.nexo.api.feature.IBlock;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.feature.MinecraftBlock;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FabricNexoModelLoader extends NexoModelLoader {

    @Override
    public void init(Nexo nexo) {
        Map<Block, ResourceLocation> blockToModel = new HashMap<>();
        Map<ResourceLocation, NexoModel> unbakedModels = new HashMap<>();

        Iterable<Map.Entry<Location, IBlock>> blocksWithModels = nexo.getFeatureRegistry(IBlock.class)
                .entrySet()
                .stream()
                .filter(e -> e.getValue() instanceof IBlock block && block.model() != null)
                .map(e -> Map.entry(e.getKey(), (IBlock) e.getValue()))::iterator;

        for (Map.Entry<Location, IBlock> entry : blocksWithModels) {
            Location blockId = entry.getKey();
            IBlock block = entry.getValue();
            Model model = block.model();
            for (Location texture : model.textures().values()) {
                NexoMod mod = nexo.getMod(texture.namespace());
                Path filePath = mod.path().resolve(texture.path());
                if (Files.isRegularFile(filePath)) {
                    NexoAtlas.register(NexoAtlas.BLOCK_ATLAS, texture, filePath);
                }else{
                    try {
                        FileSystem jarFs = FileSystems.newFileSystem(mod.path(), (ClassLoader) null);
                        Path jarPath = jarFs.getPath(texture.path());
                        if(Files.isRegularFile(jarPath)) {
                            NexoAtlas.register(NexoAtlas.BLOCK_ATLAS, texture, jarPath);
                        }
                    } catch (IOException e) {
                        NexoMinecraft.LOGGER.error("Failed to read from JAR {}", mod.path(), e);
                    }
                }
            }
            ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(blockId.namespace(), "block/" + blockId.path());
            blockToModel.put(((MinecraftBlock) block).getHolder().value(), modelId);
            unbakedModels.put(modelId, new NexoModel(model));
        }

        ModelLoadingPlugin.register(pluginContext -> {
            // Block state resolvers: map each block state to our model
            for (var entry : blockToModel.entrySet()) {
                Block block = entry.getKey();
                ResourceLocation modelId = entry.getValue();

                pluginContext.registerBlockStateResolver(block, context -> {
                    block.getStateDefinition().getPossibleStates().forEach(state -> {
                        context.setModel(state, context.getOrLoadModel(modelId));
                    });
                });
            }

            // Model resolver: return our UnbakedModel when the system asks for it
            pluginContext.resolveModel().register(context -> unbakedModels.get(context.id()));
        });
    }

}
