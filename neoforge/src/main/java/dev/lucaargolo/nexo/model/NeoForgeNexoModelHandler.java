package dev.lucaargolo.nexo.model;

import dev.lucaargolo.nexo.NeoForgeNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NeoForgeNexoModelHandler extends NexoModelHandler<NeoForgeNexoMinecraft> {

    private static final Map<ResourceLocation, UnbakedModel> CUSTOM_MODELS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, UnbakedModel> BLOCK_MODELS = new ConcurrentHashMap<>();

    private final Set<ResourceLocation> itemModelIds = new HashSet<>();

    public NeoForgeNexoModelHandler(NeoForgeNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public void init() {
        super.init();

        this.nexo().modBus().addListener(ModelEvent.RegisterAdditional.class, event -> {
            for (ResourceLocation modelId : itemModelIds) {
                event.register(new ModelResourceLocation(modelId, ModelResourceLocation.STANDALONE_VARIANT));
            }
        });
    }

    @Override
    protected void collectModel(Feature<?> feature, Model model, ResourceLocation modelId, NexoMinecraftModel mcModel) {
        CUSTOM_MODELS.put(modelId, mcModel);
        if (feature instanceof BlockBase) {
            Location location = feature.location();
            ResourceLocation blockKey = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
            BLOCK_MODELS.put(blockKey, mcModel);
        } else if (feature instanceof ItemBase) {
            itemModelIds.add(modelId);
        }
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
