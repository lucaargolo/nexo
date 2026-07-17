package dev.lucaargolo.nexo.render.model;

import dev.lucaargolo.nexo.render.MinecraftBakedGraphics3D;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FabricNexoBakedModel<M, U> extends NexoBakedModel<M, U> implements FabricBakedModel {

    private final @NotNull RenderMaterial material;

    public FabricNexoBakedModel(@NotNull NexoUnbakedModel<M, U> model, @NotNull Function<Material, TextureAtlasSprite> textureGetter, @NotNull Matrix4f modelTransform, boolean ambientOcclusion, @NotNull ItemTransforms transforms, @NotNull TextureAtlasSprite particle) {
        super(model, textureGetter, modelTransform, ambientOcclusion, transforms, particle);
        Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        if (renderer == null) {
            throw new IllegalStateException("Fabric Renderer API is not initialized");
        }
        this.material = renderer.materialFinder().find();
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitItemQuads(
            @NotNull ItemStack stack,
            @NotNull Supplier<RandomSource> randomSupplier,
            @NotNull RenderContext context
    ) {
        if(model.type == ItemStack.class) {
            QuadEmitter emitter = context.getEmitter();
            List<BakedQuad> quads = MinecraftBakedGraphics3D.bake(
                    model.renderer,
                    model.factory.apply(model.type.cast(stack)),
                    textureGetter,
                    modelTransform
            ).quads();
            for (BakedQuad quad : quads) {
                emitter.fromVanilla(quad, material, null).emit();
            }
        }else {
            super.emitItemQuads(stack, randomSupplier, context);
        }

    }
}
