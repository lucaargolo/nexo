package dev.lucaargolo.nexo.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.lucaargolo.nexo.NexoAtlas;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.event.Event;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.render.model.NexoUnbakedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public abstract class NexoRenderingHandler<N extends NexoMinecraft> {

    private final N nexo;
    protected final NexoAtlas nexoAtlas = new NexoAtlas();
    protected final MinecraftShaderRenderer shaderRenderer = new MinecraftShaderRenderer();

    public NexoRenderingHandler(N nexo) {
        this.nexo = nexo;
    }

    public N nexo() {
        return nexo;
    }

    public void init() {
        nexo.on(FeatureRegisteredEvent.class, Event.Priority.NORMAL, event -> {
            Feature<?> feature = event.value();
            switch (feature) {
                case BlockBase block -> {
                    StaticRenderer<Graphics3D, BlockUnit<?>> renderer = block.renderer();
                    if (renderer != null && renderer.resolved()) {
                        ResourceLocation modelId = modelId(event.location(), feature);
                        this.registerTextures(nexo, renderer.materials().values(), NexoAtlas.BLOCK_ATLAS);
                        this.collectModel(feature, modelId, () -> new NexoUnbakedModel<>(
                                nexo,
                                BlockState.class,
                                MinecraftFeatureType.BLOCK.convert(block).defaultBlockState(),
                                nexo::stateToUnit,
                                renderer
                        ));
                    }
                }
                case ItemBase item -> {
                    Renderer<Graphics3D, ItemUnit<?>> renderer = item.renderer();
                    if (renderer != null && renderer.resolved()) {
                        ResourceLocation modelId = modelId(event.location(), feature);
                        if (renderer instanceof StaticRenderer<Graphics3D, ItemUnit<?>> staticRenderer) {
                            this.registerTextures(nexo, renderer.materials().values(), NexoAtlas.BLOCK_ATLAS);
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
                }
                case EntityBase entity -> {
                    Renderer<Graphics3D, EntityUnit<?>> renderer = entity.renderer();
                    if (renderer != null && renderer.resolved()) {
                        this.registerTextures(nexo, renderer.materials().values(), NexoAtlas.BLOCK_ATLAS);
                    }
                    this.registerEntityRenderer(entity);
                }
                default -> {}
            }
            return true;
        });
    }

    protected abstract void collectModel(@NotNull Feature<?> feature, @NotNull ResourceLocation modelId, @NotNull Supplier<UnbakedModel> model);

    public abstract void registerResourceModel(@NotNull ResourceLocation modelId, @NotNull Supplier<UnbakedModel> model);

    protected abstract void registerItemRenderer(ItemBase item);

    protected ItemRenderer createItemRenderer(NexoMinecraft nexo, ItemBase base) {
        Renderer<Graphics3D, ItemUnit<?>> renderer = base.renderer();
        if(renderer == null) {
            return ItemRenderer.EMPTY;
        }else{
            return (stack, mode, matrices, vertexConsumers, light, overlay) -> {
                MinecraftGraphics3D graphics = new MinecraftGraphics3D(matrices, vertexConsumers, shaderRenderer, light, overlay);
                try {
                    renderer.render(graphics, nexo.stackToUnit(stack));
                } finally {
                    graphics.finish();
                }
            };
        }
    }

    protected abstract void registerEntityRenderer(EntityBase entity);

    protected <T extends Entity> void registerEntityRenderer(NexoMinecraft nexo, EntityType<T> type, EntityBase base, BiConsumer<EntityType<T>, EntityRendererProvider<T>> registrar) {
        Renderer<Graphics3D, EntityUnit<?>> renderer = base.renderer();
        if(renderer == null) {
            registrar.accept(type, NoopRenderer::new);
        }else{
            registrar.accept(type, pContext -> new EntityRenderer<>(pContext) {
                @Override
                public void render(@NotNull T pEntity, float pEntityYaw, float pPartialTick, @NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBufferSource, int pPackedLight) {
                    super.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, pBufferSource, pPackedLight);
                    MinecraftGraphics3D graphics = new MinecraftGraphics3D(
                            pPoseStack, pBufferSource, shaderRenderer, pPackedLight, OverlayTexture.NO_OVERLAY
                    );
                    try {
                        renderer.render(graphics, nexo.entityToUnit(pEntity));
                    } finally {
                        graphics.finish();
                    }
                }

                @Override
                public @NotNull ResourceLocation getTextureLocation(@NotNull T pEntity) {
                    return InventoryMenu.BLOCK_ATLAS;
                }
            });
        }
    }

    private static ResourceLocation modelId(Location location, Feature<?> feature) {
        String prefix = switch (feature) {
            case BlockBase ignored -> "block/";
            case ItemBase ignored -> "item/";
            default -> "";
        };
        return NexoMinecraft.rl(location).withPrefix(prefix);
    }

    private void registerTextures(Nexo nexo, Collection<Material<?>> materials, Location atlas) {
        for (Material<?> material : materials) {
            Location location = material.location();
            Object data = material.data();
            if(data instanceof Location) {
                this.nexoAtlas.register(atlas, location);
            }else if(data instanceof byte[] array) {
                this.nexoAtlas.register(atlas, location, array);
            }
        }
    }

    @FunctionalInterface
    protected interface ItemRenderer {
        ItemRenderer EMPTY = (stack, mode, matrices, vertexConsumers, light, overlay) -> {

        };

        void render(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay);
    }

}
