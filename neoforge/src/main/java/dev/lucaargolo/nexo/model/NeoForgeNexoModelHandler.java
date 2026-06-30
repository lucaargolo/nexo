package dev.lucaargolo.nexo.model;

import dev.lucaargolo.nexo.NeoForgeNexoMinecraft;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.block.IBlock;
import dev.lucaargolo.nexo.api.feature.item.IItem;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class NeoForgeNexoModelHandler extends NexoModelHandler {

    @Override
    public void init(Nexo nexo) {
        Map<ResourceLocation, ResourceLocation> blockToModel = new HashMap<>();
        Map<ResourceLocation, NexoMinecraftModel> unbakedModels = new HashMap<>();
        Map<ResourceLocation, ResourceLocation> itemToModel = new HashMap<>();

        NeoForgeNexoMinecraft neoForgeNexo = (NeoForgeNexoMinecraft) nexo;

        collectFeatureModels(nexo, IBlock.class, "block/", unbakedModels,
                (blockId, block, model, modelId) ->
                        blockToModel.put(
                                ResourceLocation.fromNamespaceAndPath(blockId.namespace(), blockId.path()),
                                modelId));

        collectFeatureModels(nexo, IItem.class, "item/", unbakedModels,
                (itemId, item, model, modelId) ->
                        itemToModel.put(
                                ResourceLocation.fromNamespaceAndPath(itemId.namespace(), itemId.path()),
                                modelId));

        neoForgeNexo.getModBus().addListener(ModelEvent.ModifyBakingResult.class, event -> {
            Function<Material, TextureAtlasSprite> textureGetter = event.getTextureGetter();
            NexoMinecraft.LOGGER.info("NeoForgeNexoModelLoader: injecting {} block models, {} item models",
                    blockToModel.size(), itemToModel.size());

            ModelBaker baker = new ModelBaker() {
                @Override @Nullable public UnbakedModel getModel(@NotNull ResourceLocation l) { return null; }
                @Override @Nullable public BakedModel bake(@NotNull ResourceLocation l, @NotNull ModelState s) { return null; }
                @Override @Nullable public UnbakedModel getTopLevelModel(@NotNull ModelResourceLocation l) { return null; }
                @Override @Nullable public BakedModel bake(@NotNull ResourceLocation l, @NotNull ModelState s, @NotNull Function<Material, TextureAtlasSprite> sp) { return null; }
                @Override @Nullable public BakedModel bakeUncached(UnbakedModel m, @NotNull ModelState s, @NotNull Function<Material, TextureAtlasSprite> sp) {
                    return m.bake(this, sp, s);
                }
                @Override @NotNull public Function<Material, TextureAtlasSprite> getModelTextureGetter() { return textureGetter; }
            };

            for (var bEntry : blockToModel.entrySet()) {
                ResourceLocation blockRl = bEntry.getKey();
                ResourceLocation modelId = bEntry.getValue();
                NexoMinecraftModel unbakedModel = unbakedModels.get(modelId);
                if (unbakedModel == null) continue;

                Block block = BuiltInRegistries.BLOCK.get(blockRl);
                if (block == null) {
                    NexoMinecraft.LOGGER.warn("Block not found in registry: {}", blockRl);
                    continue;
                }

                BakedModel bakedModel = unbakedModel.bake(baker, textureGetter, BlockModelRotation.X0_Y0);
                block.getStateDefinition().getPossibleStates().forEach(state ->
                        event.getModels().put(BlockModelShaper.stateToModelLocation(state), bakedModel));
            }

            for (var iEntry : itemToModel.entrySet()) {
                ResourceLocation itemRl = iEntry.getKey();
                ResourceLocation modelId = iEntry.getValue();
                NexoMinecraftModel unbakedModel = unbakedModels.get(modelId);
                if (unbakedModel == null) continue;

                if (!BuiltInRegistries.ITEM.containsKey(itemRl)) {
                    NexoMinecraft.LOGGER.warn("Item not found in registry: {}", itemRl);
                    continue;
                }

                BakedModel bakedModel = unbakedModel.bake(baker, textureGetter, BlockModelRotation.X0_Y0);
                event.getModels().put(ModelResourceLocation.inventory(itemRl), bakedModel);
            }
        });
    }

}
