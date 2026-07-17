package dev.lucaargolo.nexo.render.model;

import dev.lucaargolo.nexo.render.MinecraftBakedGraphics3D;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;
import java.util.function.Function;

public final class NeoForgeNexoBakedModel<M, U> extends NexoBakedModel<M, U> {

    public NeoForgeNexoBakedModel(@NotNull NexoUnbakedModel<M, U> model, @NotNull Function<Material, TextureAtlasSprite> textureGetter, @NotNull Matrix4f modelTransform, boolean ambientOcclusion, @NotNull ItemTransforms transforms, @NotNull TextureAtlasSprite particle) {
        super(model, textureGetter, modelTransform, ambientOcclusion, transforms, particle);
    }

    @Override
    public @NotNull List<BakedModel> getRenderPasses(@NotNull ItemStack stack, boolean fabulous) {
        if(model.type == ItemStack.class) {
            return List.of(new BakedModelWrapper<>(this) {
                @Override
                public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
                    return MinecraftBakedGraphics3D.bake(
                            model.renderer,
                            model.factory.apply(model.type.cast(stack)),
                            textureGetter,
                            modelTransform
                    ).quads();
                }
            });
        } else {
            return super.getRenderPasses(stack, fabulous);
        }
    }
}
