package dev.lucaargolo.nexo.render;

import dev.lucaargolo.nexo.FabricNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.event.SpriteAtlasStitchCallback;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.feature.block.MinecraftBlock;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class FabricMinecraftRenderingHandler extends MinecraftRenderingHandler<FabricNexoMinecraft> {

    private final Map<Block, ResourceLocation> blockToModel = new HashMap<>();
    private final Map<ResourceLocation, UnbakedModel> unbakedModels = new HashMap<>();
    private final Set<ResourceLocation> itemModelIds = new HashSet<>();

    public FabricMinecraftRenderingHandler(FabricNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public void init() {
        super.init();

        WorldRenderEvents.START.register(context -> shaderRenderer.beginFrame());
        WorldRenderEvents.LAST.register(context -> shaderRenderer.endFrame());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shaderRenderer.close());
        SpriteAtlasStitchCallback.EVENT.register((atlas, registered, embedded) -> {
            registered.addAll(minecraftAtlas.getRegistered(atlas));
            embedded.addAll(minecraftAtlas.getEmbedded(atlas));
            return this.nexo();
        });

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
    protected void collectModel(@NotNull Feature<?, ?> feature, @NotNull ResourceLocation modelId, @NotNull Supplier<UnbakedModel> model) {
        registerModel(modelId, model);
        if (feature instanceof BlockBase block) {
            blockToModel.put(MinecraftFeatureType.BLOCK.convert(block), modelId);
        } else if (feature instanceof ItemBase) {
            itemModelIds.add(modelId);
        }
    }

    @Override
    public void registerModel(@NotNull ResourceLocation modelId, @NotNull Supplier<UnbakedModel> model) {
        unbakedModels.put(modelId, model.get());
    }

    @Override
    protected void registerItemRenderer(ItemBase item) {
        if (this.nexo().getSide() != Side.CLIENT) {
            return;
        }
        ItemRenderer renderer = createItemRenderer(this.nexo(), item);
        BuiltinItemRendererRegistry.INSTANCE.register(MinecraftFeatureType.ITEM.convert(item), renderer::render);
    }

    @Override
    public void registerBlockRenderer(BlockBase block) {
        if (this.nexo().getSide() != Side.CLIENT) {
            return;
        }
        BlockEntityType<?> type = MinecraftBlock.CONVERT_ENTITY.forward(block).value();
        this.registerBlockRenderer(type, block, BlockEntityRenderers::register);
    }

    @Override
    protected void registerEntityRenderer(EntityBase entity) {
        if (this.nexo().getSide() != Side.CLIENT) {
            return;
        }
        EntityType<? extends Entity> type = MinecraftFeatureType.ENTITY.convert(entity);
        registerEntityRenderer(type, entity, EntityRendererRegistry::register);
    }

}
