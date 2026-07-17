package dev.lucaargolo.nexo.render.model;

import dev.lucaargolo.nexo.render.MinecraftBakedGraphics3D;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;
import java.util.function.Function;

public abstract class NexoBakedModel<M, U> implements BakedModel {

    protected final @NotNull NexoUnbakedModel<M, U> model;
    protected final @NotNull Function<Material, TextureAtlasSprite> textureGetter;
    protected final @NotNull Matrix4f modelTransform;
    protected final boolean ambientOcclusion;
    protected final @NotNull ItemTransforms transforms;
    protected final @NotNull TextureAtlasSprite particle;

    public NexoBakedModel(
            @NotNull NexoUnbakedModel<M, U> model,
            @NotNull Function<Material, TextureAtlasSprite> textureGetter,
            @NotNull Matrix4f modelTransform,
            boolean ambientOcclusion,
            @NotNull ItemTransforms transforms,
            @NotNull TextureAtlasSprite particle
    ) {
        this.model = model;
        this.textureGetter = textureGetter;
        this.modelTransform = new Matrix4f(modelTransform);
        this.ambientOcclusion = ambientOcclusion;
        this.transforms = transforms;
        this.particle = particle;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            @NotNull RandomSource random
    ) {
        if(model.type == BlockState.class && side == null) {
            return MinecraftBakedGraphics3D.bake(
                    model.renderer,
                    model.factory.apply(state != null ? model.type.cast(state) : model.base),
                    textureGetter,
                    modelTransform
            ).quads();
        } else {
            return List.of();
        }
    }

    @Override
    public final boolean useAmbientOcclusion() {
        return ambientOcclusion;
    }

    @Override
    public final boolean isGui3d() {
        return true;
    }

    @Override
    public final boolean usesBlockLight() {
        return true;
    }

    @Override
    public final boolean isCustomRenderer() {
        return false;
    }

    @Override
    public final @NotNull TextureAtlasSprite getParticleIcon() {
        return particle;
    }

    @Override
    public final @NotNull ItemTransforms getTransforms() {
        return transforms;
    }

    @Override
    public final @NotNull ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
