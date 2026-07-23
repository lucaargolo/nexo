package dev.lucaargolo.nexo.render;

import dev.lucaargolo.nexo.FabricNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.event.SpriteAtlasStitchCallback;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class FabricNexoRenderingHandler extends NexoRenderingHandler<FabricNexoMinecraft> {

    private final Map<Block, ResourceLocation> blockToModel = new HashMap<>();
    private final Map<ResourceLocation, UnbakedModel> unbakedModels = new HashMap<>();
    private final Set<ResourceLocation> itemModelIds = new HashSet<>();

    public FabricNexoRenderingHandler(FabricNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public void init() {
        super.init();

        WorldRenderEvents.START.register(context -> shaderRenderer.beginFrame());
        WorldRenderEvents.LAST.register(context -> shaderRenderer.endFrame());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shaderRenderer.close());
        SpriteAtlasStitchCallback.EVENT.register((atlas, registered, embedded) -> {
            registered.addAll(nexoAtlas.getRegistered(atlas));
            embedded.putAll(nexoAtlas.getEmbedded(atlas));
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
        registerResourceModel(modelId, model);
        if (feature instanceof BlockBase block) {
            blockToModel.put(MinecraftFeatureType.BLOCK.convert(block), modelId);
        } else if (feature instanceof ItemBase) {
            itemModelIds.add(modelId);
        }
    }

    @Override
    public void registerResourceModel(@NotNull ResourceLocation modelId, @NotNull Supplier<UnbakedModel> model) {
        unbakedModels.put(modelId, model.get());
    }

    @Override
    protected void registerItemRenderer(ItemBase item) {
        ItemRenderer renderer = createItemRenderer(this.nexo(), item);
        BuiltinItemRendererRegistry.INSTANCE.register(MinecraftFeatureType.ITEM.convert(item), renderer::render);
    }

    @Override
    protected void registerEntityRenderer(EntityBase entity) {
        EntityType<? extends Entity> entityType = MinecraftFeatureType.ENTITY.convert(entity);
        registerEntityRenderer(this.nexo(), entityType, entity, EntityRendererRegistry::register);
    }

}
