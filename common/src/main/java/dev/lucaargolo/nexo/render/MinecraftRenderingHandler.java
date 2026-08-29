package dev.lucaargolo.nexo.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.event.Event;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.render.model.ModelRenderer;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.render.atlas.MinecraftAtlasHandler;
import dev.lucaargolo.nexo.render.model.NexoUnbakedModel;
import dev.lucaargolo.nexo.render.shader.MinecraftShaderHandler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class MinecraftRenderingHandler<N extends NexoMinecraft<N, ?, ?, ?>> {

    protected final N nexo;

    protected final MinecraftAtlasHandler atlasHandler;
    protected final MinecraftShaderHandler shaderHandler;

    public MinecraftRenderingHandler(N nexo) {
        this.nexo = nexo;
        this.atlasHandler = new MinecraftAtlasHandler(nexo);
        this.shaderHandler = new MinecraftShaderHandler();
    }

    public MinecraftAtlasHandler atlasHandler() {
        return atlasHandler;
    }

    public MinecraftShaderHandler shaderHandler() {
        return shaderHandler;
    }

    public void init() {
        nexo.on(FeatureRegisteredEvent.class, Event.Priority.NORMAL, event -> {
            Feature<?, ?> feature = event.value();
            switch (feature) {
                case BlockBase block -> {
                    Renderer<Graphics3D, BlockUnit<?>> renderer = block.renderer();
                    ResourceLocation modelId = modelId(event.location(), feature);
                    if (renderer != null && renderer.resolved()) {
                        this.registerMaterials(nexo, MinecraftAtlasHandler.BLOCK_ATLAS, renderer.materials().values());
                    }
                    if (renderer instanceof StaticRenderer<Graphics3D, BlockUnit<?>> staticRenderer) {
                        this.collectModel(feature, modelId, () -> new NexoUnbakedModel<>(
                                nexo,
                                BlockState.class,
                                MinecraftFeatureType.BLOCK.convert(block).defaultBlockState(),
                                nexo::stateToUnit,
                                staticRenderer
                        ));
                    } else {
                        this.collectModel(feature, modelId, () -> NexoUnbakedModel.builtin(renderer));
                        this.registerBlockRenderer(block);
                    }
                }
                case ItemBase item -> {
                    Renderer<Graphics3D, ItemUnit<?>> renderer = item.renderer();
                    ResourceLocation modelId = modelId(event.location(), feature);
                    if (renderer != null && renderer.resolved()) {
                        this.registerMaterials(nexo, MinecraftAtlasHandler.BLOCK_ATLAS, renderer.materials().values());
                    }
                    if (renderer instanceof StaticRenderer<Graphics3D, ItemUnit<?>> staticRenderer) {
                        this.collectModel(feature, modelId, () -> new NexoUnbakedModel<>(
                                nexo,
                                ItemStack.class,
                                MinecraftFeatureType.ITEM.convert(item).getDefaultInstance(),
                                nexo::stackToUnit,
                                staticRenderer
                        ));
                    } else {
                        this.collectModel(feature, modelId, () -> NexoUnbakedModel.builtin(renderer));
                        this.registerItemRenderer(item);
                    }
                }
                case EntityBase entity -> {
                    Renderer<Graphics3D, EntityUnit<?>> renderer = entity.renderer();
                    if (renderer != null && renderer.resolved()) {
                        this.registerMaterials(nexo, MinecraftAtlasHandler.ENTITY_ATLAS, renderer.materials().values());
                    }
                    this.registerEntityRenderer(entity);
                }
                case ScreenBase screen -> {
                    if (screen.resolved()) {
                        this.registerMaterials(nexo, MinecraftAtlasHandler.SCREEN_ATLAS, screen.materials().values());
                    }
                }
                default -> {}
            }
            return true;
        });
    }

    public void registerModel(@NotNull ModelResource resource) {
        ModelRenderer<ModelResource> renderer = new ModelRenderer<>(resource);
        registerMaterials(nexo, MinecraftAtlasHandler.BLOCK_ATLAS, renderer.materials().values());
        registerModel(
                NexoMinecraft.rl(resource.location().withoutExtension()),
                () -> new NexoUnbakedModel<>(nexo, ModelResource.class, resource, Function.identity(), renderer)
        );
    }

    public abstract void registerModel(@NotNull ResourceLocation modelId, @NotNull Supplier<UnbakedModel> model);

    protected abstract void collectModel(@NotNull Feature<?, ?> feature, @NotNull ResourceLocation modelId, @NotNull Supplier<UnbakedModel> model);

    protected abstract void registerItemRenderer(ItemBase item);

    protected ItemRenderer createItemRenderer(NexoMinecraft<N, ?, ?, ?> nexo, ItemBase base) {
        Renderer<Graphics3D, ItemUnit<?>> renderer = base.renderer();
        if(renderer == null) {
            return ItemRenderer.EMPTY;
        }else{
            return (stack, mode, matrices, vertexConsumers, light, overlay) -> {
                DynamicMinecraftGraphics3D graphics = new DynamicMinecraftGraphics3D(nexo, matrices, vertexConsumers, light, overlay);
                try {
                    renderer.render(graphics, nexo.stackToUnit(stack));
                } finally {
                    graphics.finish();
                }
            };
        }
    }

    protected abstract void registerBlockRenderer(BlockBase block);

    protected <T extends BlockEntity> void registerBlockRenderer(BlockEntityType<T> type, BlockBase base, BiConsumer<BlockEntityType<T>, BlockEntityRendererProvider<T>> registrar) {
        Renderer<Graphics3D, BlockUnit<?>> renderer = base.renderer();
        if(renderer != null) {
            registrar.accept(type, (context) -> (blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay) -> {
                DynamicMinecraftGraphics3D graphics = new DynamicMinecraftGraphics3D(nexo, poseStack, bufferSource, packedLight, packedOverlay);
                try {
                    renderer.render(graphics, nexo.blockToUnit(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity));
                } finally {
                    graphics.finish();
                }
            });
        }
    }

    protected abstract void registerEntityRenderer(EntityBase entity);

    protected <T extends Entity> void registerEntityRenderer(EntityType<T> type, EntityBase base, BiConsumer<EntityType<T>, EntityRendererProvider<T>> registrar) {
        Renderer<Graphics3D, EntityUnit<?>> renderer = base.renderer();
        if(renderer == null) {
            registrar.accept(type, NoopRenderer::new);
        }else{
            registrar.accept(type, pContext -> new EntityRenderer<>(pContext) {
                @Override
                public void render(@NotNull T pEntity, float pEntityYaw, float pPartialTick, @NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBufferSource, int pPackedLight) {
                    super.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, pBufferSource, pPackedLight);
                    DynamicMinecraftGraphics3D graphics = new DynamicMinecraftGraphics3D(
                            nexo,
                            pPoseStack,
                            pBufferSource,
                            pPackedLight,
                            OverlayTexture.NO_OVERLAY
                    );
                    try {
                        renderer.render(graphics, nexo.entityToUnit(pEntity));
                    } finally {
                        graphics.finish();
                    }
                }

                @Override
                public @NotNull ResourceLocation getTextureLocation(@NotNull T pEntity) {
                    return NexoMinecraft.rl(MinecraftAtlasHandler.ENTITY_ATLAS);
                }
            });
        }
    }

    private static ResourceLocation modelId(Location location, Feature<?, ?> feature) {
        String prefix = switch (feature) {
            case BlockBase ignored -> "block/";
            case ItemBase ignored -> "item/";
            default -> "";
        };
        return NexoMinecraft.rl(location).withPrefix(prefix);
    }

    private void registerMaterials(Nexo nexo, Location atlas, Collection<Material<?>> materials) {
        for (Material<?> material : materials) {
            this.atlasHandler.register(atlas, material);
        }
    }

    @FunctionalInterface
    protected interface ItemRenderer {
        ItemRenderer EMPTY = (stack, mode, matrices, vertexConsumers, light, overlay) -> {

        };

        void render(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay);
    }

}
