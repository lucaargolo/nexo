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
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.render.model.ModelRenderer;
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public abstract class NexoRenderingHandler<N extends NexoMinecraft> {

    private final N nexo;

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
                    if (renderer == null) return true;
                    ResourceLocation modelId = modelId(event.location(), feature);
                    registerTextures(nexo, renderer.textures().values(), NexoAtlas.BLOCK_ATLAS);
                    registerEmbeddedTextures(renderer, NexoAtlas.BLOCK_ATLAS);
                    collectModel(feature, modelId, () -> new NexoUnbakedModel<>(
                        nexo,
                        BlockState.class,
                        MinecraftFeatureType.BLOCK.convert(block).defaultBlockState(),
                        nexo::stateToUnit,
                        renderer
                    ));
                }
                case ItemBase item -> {
                    Renderer<Graphics3D, ItemUnit<?>> renderer = item.renderer();
                    if (renderer == null) return true;
                    ResourceLocation modelId = modelId(event.location(), feature);
                    if (renderer instanceof StaticRenderer<Graphics3D, ItemUnit<?>> staticRenderer) {
                        registerTextures(nexo, renderer.textures().values(), NexoAtlas.BLOCK_ATLAS);
                        registerEmbeddedTextures(renderer, NexoAtlas.BLOCK_ATLAS);
                        collectModel(feature, modelId, () -> new NexoUnbakedModel<>(
                                nexo,
                                ItemStack.class,
                                MinecraftFeatureType.ITEM.convert(item).getDefaultInstance(),
                                nexo::stackToUnit,
                                staticRenderer
                        ));
                    } else {
                        collectModel(feature, modelId, () -> NexoUnbakedModel.builtin(renderer));
                        registerItemRenderer(item);
                    }
                }
                case EntityBase entity -> {
                    Renderer<Graphics3D, EntityUnit<?>> renderer = entity.renderer();
                    if (renderer != null) {
                        registerTextures(nexo, renderer.textures().values(), NexoAtlas.BLOCK_ATLAS);
                        registerEmbeddedTextures(renderer, NexoAtlas.BLOCK_ATLAS);
                    }
                    registerEntityRenderer(entity);
                }
                default -> {}
            }
            return true;
        });
    }

    protected abstract void collectModel(Feature<?> feature, ResourceLocation modelId, Supplier<UnbakedModel> mcModel);

    protected abstract void registerItemRenderer(ItemBase item);

    protected static ItemRenderer createItemRenderer(NexoMinecraft nexo, ItemBase base) {
        Renderer<Graphics3D, ItemUnit<?>> renderer = base.renderer();
        if(renderer == null) {
            return ItemRenderer.EMPTY;
        }else{
            return (stack, mode, matrices, vertexConsumers, light, overlay) -> {
                MinecraftGraphics3D graphics = new MinecraftGraphics3D(matrices, vertexConsumers, light, overlay);
                try {
                    renderer.render(graphics, nexo.stackToUnit(stack));
                } finally {
                    graphics.finish();
                }
            };
        }
    }

    protected abstract void registerEntityRenderer(EntityBase entity);

    protected static <T extends Entity> void registerEntityRenderer(NexoMinecraft nexo, EntityType<T> type, EntityBase base, BiConsumer<EntityType<T>, EntityRendererProvider<T>> registrar) {
        Renderer<Graphics3D, EntityUnit<?>> renderer = base.renderer();
        if(renderer == null) {
            registrar.accept(type, NoopRenderer::new);
        }else{
            registrar.accept(type, pContext -> new EntityRenderer<>(pContext) {
                @Override
                public void render(@NotNull T pEntity, float pEntityYaw, float pPartialTick, @NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBufferSource, int pPackedLight) {
                    super.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, pBufferSource, pPackedLight);
                    MinecraftGraphics3D graphics = new MinecraftGraphics3D(pPoseStack, pBufferSource, pPackedLight, OverlayTexture.NO_OVERLAY);
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

    private static void registerTextures(Nexo nexo, Collection<Location> textures, Location atlas) {
        for (Location texture : textures) {
            registerTexture(nexo, texture, atlas);
        }
    }

    private static void registerEmbeddedTextures(Renderer<?, ?> renderer, Location atlas) {
        if (renderer instanceof ModelRenderer<?> modelRenderer) {
            modelRenderer.model().embeddedTextures().forEach(
                    (texture, data) -> NexoAtlas.register(atlas, texture, data)
            );
        }
    }

    private static void registerTexture(Nexo nexo, Location texture, Location atlas) {
        Nexo.Mod mod = nexo.getMod(texture.namespace());
        if (mod == null) return;
        Path filePath = mod.path().resolve(texture.path());
        if (!Files.isRegularFile(filePath)) {
            URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(texture.path());
            if (resourceUrl != null && "file".equals(resourceUrl.getProtocol())) {
                try {
                    filePath = Path.of(resourceUrl.toURI());
                } catch (URISyntaxException ignored) {
                }
            }
        }
        if (Files.isRegularFile(filePath)) {
            NexoAtlas.register(atlas, texture, filePath);
            return;
        }
        try {
            URI jarUri = URI.create("jar:" + mod.path().toUri());
            FileSystem jarFs;
            try {
                jarFs = FileSystems.getFileSystem(jarUri);
            } catch (FileSystemNotFoundException ignored) {
                jarFs = FileSystems.newFileSystem(jarUri, Map.of());
            }
            Path jarPath = jarFs.getPath("/", texture.path());
            if (Files.isRegularFile(jarPath)) {
                NexoAtlas.register(atlas, texture, jarPath);
                return;
            }
        } catch (IOException e) {
            NexoMinecraft.LOGGER.error("Failed to read from JAR {}", mod.path(), e);
        }
        NexoMinecraft.LOGGER.warn("Failed to locate texture {} in mod {}", texture, mod.path());
    }

    @FunctionalInterface
    protected interface ItemRenderer {

        ItemRenderer EMPTY = (stack, mode, matrices, vertexConsumers, light, overlay) -> {

        };

        void render(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay);

    }

}
