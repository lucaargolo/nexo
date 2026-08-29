package dev.lucaargolo.nexo.render.model;

import dev.lucaargolo.nexo.api.render.util.LayerMode;
import dev.lucaargolo.nexo.render.BakedMinecraftGraphics3D;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.NamedRenderTypeManager;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class NeoForgeNexoBakedModel<M, U> extends NexoBakedModel<M, U> {

    private final Map<BlockState, BakedMinecraftGraphics3D> bakedByState = Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<Item, BakedMinecraftGraphics3D> bakedByItem = new ConcurrentHashMap<>();

    public NeoForgeNexoBakedModel(@NotNull NexoUnbakedModel<M, U> model, @NotNull Function<Material, TextureAtlasSprite> textureGetter, @NotNull Matrix4f modelTransform, boolean ambientOcclusion, @NotNull ItemTransforms transforms, @NotNull TextureAtlasSprite particle) {
        super(model, textureGetter, modelTransform, ambientOcclusion, transforms, particle);
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource random, @NotNull ModelData data) {
        if (model.type != BlockState.class) {
            return super.getRenderTypes(state, random, data);
        }
        EnumSet<LayerMode> layers = EnumSet.noneOf(LayerMode.class);
        model.renderer.materials().values().forEach(material -> layers.add(material.layerMode()));
        if (layers.isEmpty()) {
            layers.add(LayerMode.SOLID);
        }
        List<RenderType> renderTypes = new ArrayList<>(LayerMode.values().length);
        for (LayerMode layerMode : layers) {
            renderTypes.add(switch (layerMode) {
                case SOLID -> RenderType.solid();
                case CUTOUT -> RenderType.cutout();
                case TRANSLUCENT -> RenderType.translucent();
            });
        }
        return ChunkRenderTypeSet.of(renderTypes);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource random, @NotNull ModelData data, @Nullable RenderType renderType) {
        if (model.type != BlockState.class) {
            return List.of();
        }
        BakedMinecraftGraphics3D graphics = bakedByState.computeIfAbsent(state, ignored -> bake(state != null ? model.type.cast(state) : model.base));
        if (renderType == null) {
            return graphics.quads(side);
        }
        if (renderType == RenderType.solid()) {
            return graphics.quads(LayerMode.SOLID, side);
        }
        if (renderType == RenderType.cutout()) {
            return graphics.quads(LayerMode.CUTOUT, side);
        }
        if (renderType == RenderType.translucent()) {
            return graphics.quads(LayerMode.TRANSLUCENT, side);
        }
        return List.of();
    }

    @Override
    public @NotNull List<BakedModel> getRenderPasses(@NotNull ItemStack stack, boolean fabulous) {
        if(model.type == ItemStack.class) {
            // Static baked models are feature-level, so the bake result does not depend on the stack contents.
            BakedMinecraftGraphics3D graphics = bakedByItem.computeIfAbsent(stack.getItem(), ignored -> bake(model.type.cast(stack)));
            List<BakedModel> passes = new ArrayList<>(LayerMode.values().length);
            for (LayerMode layerMode : LayerMode.values()) {
                List<BakedQuad> quads = graphics.allQuads(layerMode);
                if (quads.isEmpty()) {
                    continue;
                }
                String name = switch (layerMode) {
                    case SOLID -> "solid";
                    case CUTOUT -> "cutout";
                    case TRANSLUCENT -> "translucent";
                };
                RenderTypeGroup group = NamedRenderTypeManager.get(ResourceLocation.withDefaultNamespace(name));
                if (group.isEmpty()) {
                    throw new IllegalStateException("Missing NeoForge render type group minecraft:" + name);
                }
                IModelBuilder<?> builder = IModelBuilder.of(ambientOcclusion, usesBlockLight(), isGui3d(), transforms, getOverrides(), particle, group);
                for (BakedQuad quad : quads) {
                    builder.addUnculledFace(quad);
                }
                passes.add(builder.build());
            }
            return List.copyOf(passes);
        } else {
            return super.getRenderPasses(stack, fabulous);
        }
    }

}
