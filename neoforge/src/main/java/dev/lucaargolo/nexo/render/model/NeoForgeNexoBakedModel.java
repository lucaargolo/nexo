package dev.lucaargolo.nexo.render.model;

import dev.lucaargolo.nexo.api.render.util.LayerMode;
import dev.lucaargolo.nexo.render.MinecraftBakedGraphics3D;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

public final class NeoForgeNexoBakedModel<M, U> extends NexoBakedModel<M, U> {

    public NeoForgeNexoBakedModel(@NotNull NexoUnbakedModel<M, U> model, @NotNull Function<Material, TextureAtlasSprite> textureGetter, @NotNull Matrix4f modelTransform, boolean ambientOcclusion, @NotNull ItemTransforms transforms, @NotNull TextureAtlasSprite particle) {
        super(model, textureGetter, modelTransform, ambientOcclusion, transforms, particle);
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(
            @NotNull BlockState state,
            @NotNull RandomSource random,
            @NotNull ModelData data
    ) {
        if (model.type != BlockState.class) return super.getRenderTypes(state, random, data);
        EnumSet<LayerMode> layers = EnumSet.noneOf(LayerMode.class);
        model.renderer.materials().values().forEach(material -> layers.add(material.layerMode()));
        if (layers.isEmpty()) layers.add(LayerMode.SOLID);
        List<RenderType> renderTypes = new ArrayList<>(LayerMode.values().length);
        for (LayerMode layerMode : layers) renderTypes.add(blockRenderType(layerMode));
        return ChunkRenderTypeSet.of(renderTypes);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            @NotNull RandomSource random,
            @NotNull ModelData data,
            @Nullable RenderType renderType
    ) {
        if (model.type != BlockState.class) {
            return List.of();
        }

        MinecraftBakedGraphics3D graphics = bakeBlock(state);
        if (renderType == null) {
            return graphics.quads(side);
        }
        LayerMode layer = layerMode(renderType);
        return layer != null ? graphics.quads(layer, side) : List.of();
    }

    @Override
    public @NotNull List<BakedModel> getRenderPasses(@NotNull ItemStack stack, boolean fabulous) {
        if(model.type == ItemStack.class) {
            MinecraftBakedGraphics3D graphics = bake(model.type.cast(stack));
            List<BakedModel> passes = new ArrayList<>(LayerMode.values().length);
            for (LayerMode layerMode : LayerMode.values()) {
                List<BakedQuad> quads = graphics.allQuads(layerMode);
                if (quads.isEmpty()) continue;
                IModelBuilder<?> builder = IModelBuilder.of(
                        ambientOcclusion,
                        usesBlockLight(),
                        isGui3d(),
                        transforms,
                        getOverrides(),
                        particle,
                        renderTypeGroup(layerMode)
                );
                for (BakedQuad quad : quads) builder.addUnculledFace(quad);
                passes.add(builder.build());
            }
            return List.copyOf(passes);
        } else {
            return super.getRenderPasses(stack, fabulous);
        }
    }

    private @NotNull MinecraftBakedGraphics3D bakeBlock(@Nullable BlockState state) {
        return bake(state != null ? model.type.cast(state) : model.base);
    }

    private static @NotNull RenderType blockRenderType(@NotNull LayerMode layerMode) {
        return switch (layerMode) {
            case SOLID -> RenderType.solid();
            case CUTOUT -> RenderType.cutout();
            case TRANSLUCENT -> RenderType.translucent();
        };
    }

    private static @Nullable LayerMode layerMode(@NotNull RenderType renderType) {
        if (renderType == RenderType.solid()) return LayerMode.SOLID;
        if (renderType == RenderType.cutout()) return LayerMode.CUTOUT;
        if (renderType == RenderType.translucent()) return LayerMode.TRANSLUCENT;
        return null;
    }

    private static @NotNull RenderTypeGroup renderTypeGroup(@NotNull LayerMode layerMode) {
        String name = switch (layerMode) {
            case SOLID -> "solid";
            case CUTOUT -> "cutout";
            case TRANSLUCENT -> "translucent";
        };
        RenderTypeGroup group = NamedRenderTypeManager.get(ResourceLocation.withDefaultNamespace(name));
        if (group.isEmpty()) throw new IllegalStateException("Missing NeoForge render type group minecraft:" + name);
        return group;
    }
}
