package dev.lucaargolo.nexo.render.model;

import dev.lucaargolo.nexo.api.render.util.LayerMode;
import dev.lucaargolo.nexo.render.MinecraftBakedGraphics3D;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.EnumMap;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FabricNexoBakedModel<M, U> extends NexoBakedModel<M, U> implements FabricBakedModel {

    private final @NotNull EnumMap<LayerMode, RenderMaterial> materials = new EnumMap<>(LayerMode.class);

    public FabricNexoBakedModel(@NotNull NexoUnbakedModel<M, U> model, @NotNull Function<Material, TextureAtlasSprite> textureGetter, @NotNull Matrix4f modelTransform, boolean ambientOcclusion, @NotNull ItemTransforms transforms, @NotNull TextureAtlasSprite particle) {
        super(model, textureGetter, modelTransform, ambientOcclusion, transforms, particle);
        Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        if (renderer == null) {
            throw new IllegalStateException("Fabric Renderer API is not initialized");
        }
        var finder = renderer.materialFinder();
        for (LayerMode layerMode : LayerMode.values()) {
            materials.put(layerMode, finder.clear().blendMode(blendMode(layerMode)).find());
        }
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(
            @NotNull BlockAndTintGetter blockView,
            @NotNull BlockState state,
            @NotNull BlockPos pos,
            @NotNull Supplier<RandomSource> randomSupplier,
            @NotNull RenderContext context
    ) {
        if (model.type == BlockState.class) {
            emit(bake(model.type.cast(state)), context);
        } else {
            super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
        }
    }

    @Override
    public void emitItemQuads(
            @NotNull ItemStack stack,
            @NotNull Supplier<RandomSource> randomSupplier,
            @NotNull RenderContext context
    ) {
        if(model.type == ItemStack.class) {
            emit(bake(model.type.cast(stack)), context);
        }else {
            super.emitItemQuads(stack, randomSupplier, context);
        }

    }

    private void emit(@NotNull MinecraftBakedGraphics3D graphics, @NotNull RenderContext context) {
        QuadEmitter emitter = context.getEmitter();
        for (LayerMode layerMode : LayerMode.values()) {
            RenderMaterial material = materials.get(layerMode);
            for (BakedQuad quad : graphics.quads(layerMode)) {
                emitter.fromVanilla(quad, material, null).emit();
            }
        }
    }

    private static @NotNull BlendMode blendMode(@NotNull LayerMode layerMode) {
        return switch (layerMode) {
            case SOLID -> BlendMode.SOLID;
            case CUTOUT -> BlendMode.CUTOUT;
            case TRANSLUCENT -> BlendMode.TRANSLUCENT;
        };
    }
}
