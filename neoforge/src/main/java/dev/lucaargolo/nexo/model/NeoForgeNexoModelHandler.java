package dev.lucaargolo.nexo.model;

import dev.lucaargolo.nexo.NeoForgeNexoMinecraft;
import dev.lucaargolo.nexo.NexoAtlas;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Location;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.NexoMod;
import dev.lucaargolo.nexo.api.feature.IBlock;
import dev.lucaargolo.nexo.api.model.Model;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class NeoForgeNexoModelHandler extends NexoModelHandler {

    @Override
    public void init(Nexo nexo) {
        Map<ResourceLocation, ResourceLocation> blockToModel = new HashMap<>();
        Map<ResourceLocation, NexoModel> unbakedModels = new HashMap<>();

        NeoForgeNexoMinecraft neoForgeNexo = (NeoForgeNexoMinecraft) nexo;

        Iterable<Map.Entry<Location, IBlock>> blocksWithModels = nexo.getFeatureRegistry(IBlock.class)
                .entrySet()
                .stream()
                .filter(e -> e.getValue() instanceof IBlock block && block.model() != null)
                .<Map.Entry<Location, IBlock>>map(e -> Map.entry(e.getKey(), (IBlock) e.getValue()))::iterator;

        for (Map.Entry<Location, IBlock> entry : blocksWithModels) {
            Location blockId = entry.getKey();
            IBlock block = entry.getValue();
            Model model = block.model();
            for (Location texture : model.textures().values()) {
                NexoMod mod = nexo.getMod(texture.namespace());
                Path filePath = mod.path().resolve(texture.path());
                if (Files.isRegularFile(filePath)) {
                    NexoAtlas.register(NexoAtlas.BLOCK_ATLAS, texture, filePath);
                } else {
                    try {
                        FileSystem jarFs = FileSystems.newFileSystem(mod.path(), (ClassLoader) null);
                        Path jarPath = jarFs.getPath(texture.path());
                        if (Files.isRegularFile(jarPath)) {
                            NexoAtlas.register(NexoAtlas.BLOCK_ATLAS, texture, jarPath);
                        }
                    } catch (IOException e) {
                        NexoMinecraft.LOGGER.error("Failed to read from JAR {}", mod.path(), e);
                    }
                }
            }
            ResourceLocation blockRl = ResourceLocation.fromNamespaceAndPath(blockId.namespace(), blockId.path());
            ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(blockId.namespace(), "block/" + blockId.path());
            blockToModel.put(blockRl, modelId);
            unbakedModels.put(modelId, new NexoModel(model));
        }

        neoForgeNexo.getModBus().addListener(ModelEvent.ModifyBakingResult.class, event -> {
            Function<Material, TextureAtlasSprite> textureGetter = event.getTextureGetter();
            NexoMinecraft.LOGGER.info("NeoForgeNexoModelLoader: injecting {} block models", blockToModel.size());

            // Minimal ModelBaker: our models have no parent, so getModel() is never called.
            ModelBaker baker = new ModelBaker() {
                @Override
                public UnbakedModel getModel(ResourceLocation location) {
                    return null;
                }

                @Override
                @Nullable
                public BakedModel bake(ResourceLocation location, ModelState modelState) {
                    return null;
                }

                @Override
                @Nullable
                public UnbakedModel getTopLevelModel(ModelResourceLocation location) {
                    return null;
                }

                @Override
                @Nullable
                public BakedModel bake(ResourceLocation location, ModelState state, Function<Material, TextureAtlasSprite> sprites) {
                    return null;
                }

                @Override
                @Nullable
                public BakedModel bakeUncached(UnbakedModel model, ModelState state, Function<Material, TextureAtlasSprite> sprites) {
                    return model.bake(this, sprites, state);
                }

                @Override
                public Function<Material, TextureAtlasSprite> getModelTextureGetter() {
                    return textureGetter;
                }
            };

            for (var bEntry : blockToModel.entrySet()) {
                ResourceLocation blockRl = bEntry.getKey();
                ResourceLocation modelId = bEntry.getValue();
                NexoModel unbakedModel = unbakedModels.get(modelId);
                if (unbakedModel == null) continue;

                Block block = BuiltInRegistries.BLOCK.get(blockRl);
                if (block == null) {
                    NexoMinecraft.LOGGER.warn("Block not found in registry: {}", blockRl);
                    continue;
                }

                BakedModel bakedModel = unbakedModel.bake(baker, textureGetter, BlockModelRotation.X0_Y0);

                block.getStateDefinition().getPossibleStates().forEach(state -> {
                    event.getModels().put(
                            BlockModelShaper.stateToModelLocation(state),
                            bakedModel
                    );
                });
            }
        });
    }

}
