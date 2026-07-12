package dev.lucaargolo.nexo.model;

import dev.lucaargolo.nexo.FabricNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.feature.block.MinecraftBlock;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FabricNexoModelHandler extends NexoModelHandler<FabricNexoMinecraft> {

    public FabricNexoModelHandler(FabricNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public void init() {
        Map<Block, ResourceLocation> blockToModel = new HashMap<>();
        Map<ResourceLocation, NexoMinecraftModel> unbakedModels = new HashMap<>();
        Set<ResourceLocation> itemModelIds = new HashSet<>();

        collectModels(this.nexo(), BlockBase.class, "block/", unbakedModels,
                (blockId, block, model, modelId) ->
                        blockToModel.put(((MinecraftBlock) block).holder().value(), modelId));

        collectModels(this.nexo(), ItemBase.class, "item/", unbakedModels,
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
