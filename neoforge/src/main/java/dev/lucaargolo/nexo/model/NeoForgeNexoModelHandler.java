package dev.lucaargolo.nexo.model;

import dev.lucaargolo.nexo.NeoForgeNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.block.BaseBlock;
import dev.lucaargolo.nexo.api.feature.item.BaseItem;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NeoForgeNexoModelHandler extends NexoModelHandler<NeoForgeNexoMinecraft> {

    private static final Map<ResourceLocation, UnbakedModel> CUSTOM_MODELS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, UnbakedModel> BLOCK_MODELS = new ConcurrentHashMap<>();

    public NeoForgeNexoModelHandler(NeoForgeNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public void init() {
        Map<ResourceLocation, NexoMinecraftModel> unbakedModels = new HashMap<>();
        Set<ResourceLocation> itemModelIds = new HashSet<>();

        collectModels(this.nexo(), BaseBlock.class, "block/", unbakedModels,
                (blockId, block, model, modelId) -> {
                    NexoMinecraftModel mcModel = unbakedModels.get(modelId);
                    CUSTOM_MODELS.put(modelId, mcModel);
                    ResourceLocation blockKey = ResourceLocation.fromNamespaceAndPath(
                            blockId.namespace(), blockId.path());
                    BLOCK_MODELS.put(blockKey, mcModel);
                });

        collectModels(this.nexo(), BaseItem.class, "item/", unbakedModels,
                (itemId, item, model, modelId) -> {
                    CUSTOM_MODELS.put(modelId, unbakedModels.get(modelId));
                    itemModelIds.add(modelId);
                });

        this.nexo().modBus().addListener(ModelEvent.RegisterAdditional.class, event -> {
            for (ResourceLocation modelId : itemModelIds) {
                event.register(new ModelResourceLocation(modelId, ModelResourceLocation.STANDALONE_VARIANT));
            }
        });
    }

    @Nullable
    public static UnbakedModel getCustomModel(ResourceLocation id) {
        return CUSTOM_MODELS.get(id);
    }

    @Nullable
    public static UnbakedModel getBlockModel(ResourceLocation blockKey) {
        return BLOCK_MODELS.get(blockKey);
    }
}
