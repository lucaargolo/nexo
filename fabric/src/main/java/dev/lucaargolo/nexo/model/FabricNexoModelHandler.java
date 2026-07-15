package dev.lucaargolo.nexo.model;

import dev.lucaargolo.nexo.FabricNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FabricNexoModelHandler extends NexoModelHandler<FabricNexoMinecraft> {

    private final Map<Block, ResourceLocation> blockToModel = new HashMap<>();
    private final Map<ResourceLocation, NexoMinecraftModel> unbakedModels = new HashMap<>();
    private final Set<ResourceLocation> itemModelIds = new HashSet<>();

    public FabricNexoModelHandler(FabricNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public void init() {
        super.init();

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

    @Override
    protected void collectModel(Feature<?> feature, Model model, ResourceLocation modelId, NexoMinecraftModel mcModel) {
        unbakedModels.put(modelId, mcModel);
        if (feature instanceof BlockBase block) {
            blockToModel.put(MinecraftFeatureType.BLOCK.crafted(block), modelId);
        } else if (feature instanceof ItemBase) {
            itemModelIds.add(modelId);
        }
    }

}
