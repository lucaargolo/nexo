package dev.lucaargolo.nexo.render.model;

import com.mojang.math.Transformation;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.util.NexoUtils;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class NexoUnbakedModel<M, U> implements UnbakedModel {

    private final @NotNull NexoMinecraft nexo;
    final @NotNull Class<M> type;
    final @NotNull M base;
    final @NotNull Function<M, U> factory;
    final @NotNull StaticRenderer<Graphics3D, U> renderer;

    public NexoUnbakedModel(
            @NotNull NexoMinecraft nexo,
            @NotNull Class<M> type,
            @NotNull M base,
            @NotNull Function<M, U> factory,
            @NotNull StaticRenderer<Graphics3D, U> renderer
    ) {
        this.nexo = nexo;
        this.type = type;
        this.base = base;
        this.factory = factory;
        this.renderer = renderer;
    }

    @Override
    public @NotNull Collection<ResourceLocation> getDependencies() {
        return List.of();
    }

    @Override
    public void resolveParents(@NotNull Function<ResourceLocation, UnbakedModel> resolver) {
    }

    @Override
    public @NotNull BakedModel bake(
            @NotNull ModelBaker baker,
            @NotNull Function<Material, TextureAtlasSprite> textureGetter,
            @NotNull ModelState modelState
    ) {
        Transformation transformation = modelState.getRotation();
        Matrix4f matrix = transformation.getMatrix();
        return NexoUtils.loadPlatformClass(this.nexo, NexoBakedModel.class,
                this,
                textureGetter,
                matrix,
                renderer.shaded(),
                getItemTransforms(renderer),
                getParticleSprite(renderer, textureGetter)
        );
    }

    private static @NotNull TextureAtlasSprite getParticleSprite(
            @NotNull Renderer<?, ?> renderer,
            @NotNull Function<Material, TextureAtlasSprite> textureGetter
    ) {
        Location location = renderer.texture("particle");
        return textureGetter.apply(new Material(
                InventoryMenu.BLOCK_ATLAS,
                location == null ? MissingTextureAtlasSprite.getLocation() : NexoMinecraft.rl(location)
        ));
    }

    public static UnbakedModel builtin(Renderer<?, ?> renderer) {
        return new UnbakedModel() {
            @Override
            public @NotNull Collection<ResourceLocation> getDependencies() {
                return List.of();
            }

            @Override
            public void resolveParents(@NotNull Function<ResourceLocation, UnbakedModel> pResolver) {

            }

            @Override
            public BakedModel bake(@NotNull ModelBaker pBaker, @NotNull Function<Material, TextureAtlasSprite> pSpriteGetter, @NotNull ModelState pState) {
                return new BuiltInModel(getItemTransforms(renderer), ItemOverrides.EMPTY, getParticleSprite(renderer, pSpriteGetter), true);
            }
        };
    }

    private static @NotNull ItemTransforms getItemTransforms(@NotNull Renderer<?, ?> renderer) {
        ItemTransform thirdPersonRight = transform(renderer, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        ItemTransform thirdPersonLeft = transform(renderer, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
        ItemTransform firstPersonRight = transform(renderer, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
        ItemTransform firstPersonLeft = transform(renderer, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        ItemTransform head = transform(renderer, ItemDisplayContext.HEAD);
        ItemTransform gui = transform(renderer, ItemDisplayContext.GUI);
        ItemTransform ground = transform(renderer, ItemDisplayContext.GROUND);
        ItemTransform fixed = transform(renderer, ItemDisplayContext.FIXED);

        if (ItemTransform.NO_TRANSFORM.equals(thirdPersonLeft)) thirdPersonLeft = thirdPersonRight;
        if (ItemTransform.NO_TRANSFORM.equals(firstPersonLeft)) firstPersonLeft = firstPersonRight;

        return new ItemTransforms(
                thirdPersonLeft,
                thirdPersonRight,
                firstPersonLeft,
                firstPersonRight,
                head,
                gui,
                ground,
                fixed
        );
    }

    private static @NotNull ItemTransform transform(
            @NotNull Renderer<?, ?> renderer,
            @NotNull ItemDisplayContext context
    ) {
        Location location = Location.of("minecraft", context.getSerializedName());
        Transform transform = renderer.transform(location);
        return new ItemTransform(
                new Vector3f(transform.rotation()),
                new Vector3f(transform.translation()).mul(0.0625F),
                new Vector3f(transform.scale())
        );
    }

}
