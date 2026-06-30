package dev.lucaargolo.nexo.model;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.block.IBlock;
import dev.lucaargolo.nexo.api.feature.item.IItem;
import dev.lucaargolo.nexo.feature.MinecraftBlock;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FabricNexoModelHandler extends NexoModelHandler {

    @Override
    public void init(Nexo nexo) {
        Map<Block, ResourceLocation> blockToModel = new HashMap<>();
        Map<ResourceLocation, NexoMinecraftModel> unbakedModels = new HashMap<>();
        Set<ResourceLocation> itemModelIds = new HashSet<>();

        collectFeatureModels(nexo, IBlock.class, "block/", unbakedModels,
                (blockId, block, model, modelId) ->
                        blockToModel.put(((MinecraftBlock) block).getHolder().value(), modelId));

        collectFeatureModels(nexo, IItem.class, "item/", unbakedModels,
                (itemId, item, model, modelId) ->
                        itemModelIds.add(modelId));

        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.addModels(itemModelIds);

            for (var entry : blockToModel.entrySet()) {
                Block block = entry.getKey();
                ResourceLocation modelId = entry.getValue();

                pluginContext.registerBlockStateResolver(block, context -> {
                    block.getStateDefinition().getPossibleStates().forEach(state -> {
                        context.setModel(state, context.getOrLoadModel(modelId));
                    });
                });
            }

            pluginContext.resolveModel().register(context -> unbakedModels.get(context.id()));
        });
    }

}
