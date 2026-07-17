package dev.lucaargolo.nexo.render.model;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.render.model.ModelRenderer;
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
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class NexoUnbakedModel<M, U> implements UnbakedModel {

    public static final UnbakedModel BUILTIN = new UnbakedModel() {
        @Override
        public @NotNull Collection<ResourceLocation> getDependencies() {
            return List.of();
        }

        @Override
        public void resolveParents(@NotNull Function<ResourceLocation, UnbakedModel> pResolver) {

        }

        @Override
        public BakedModel bake(@NotNull ModelBaker pBaker, @NotNull Function<Material, TextureAtlasSprite> pSpriteGetter, @NotNull ModelState pState) {
            //TODO
            return new BuiltInModel(ItemTransforms.NO_TRANSFORMS, ItemOverrides.EMPTY, particle(pSpriteGetter), true);
        }
    };

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
        return NexoUtils.loadPlatformClass(this.nexo, NexoBakedModel.class,
                this,
                textureGetter,
                modelState.getRotation().getMatrix(),
                ambientOcclusion(),
                itemTransforms(),
                particle(textureGetter)
        );
    }

    //TODO
    private static @NotNull TextureAtlasSprite particle(
            @NotNull Function<Material, TextureAtlasSprite> textureGetter
    ) {
        return textureGetter.apply(new Material(
                InventoryMenu.BLOCK_ATLAS,
                MissingTextureAtlasSprite.getLocation()
        ));
    }

    //TODO
    private boolean ambientOcclusion() {
        if (renderer instanceof ModelRenderer<?> modelRenderer) {
            return modelRenderer.model().shade();
        }
        return true;
    }

    //TODO
    private @NotNull ItemTransforms itemTransforms() {
        if (renderer instanceof ModelRenderer<?> modelRenderer) {
            return toItemTransforms(modelRenderer.model());
        }
        return ItemTransforms.NO_TRANSFORMS;
    }

    //TODO
    private static @NotNull ItemTransform toItemTransform(@Nullable Model.Transform transform) {
        if (transform == null) return ItemTransform.NO_TRANSFORM;
        Vector3f translation = new Vector3f(transform.translation()).mul(0.0625F);
        return new ItemTransform(
                new Vector3f(transform.rotation()),
                translation,
                new Vector3f(transform.scale())
        );
    }

    //TODO
    private static @NotNull ItemTransform getDisplayTransform(
            @NotNull Model model,
            @NotNull ItemDisplayContext context
    ) {
        Location location = Location.of("minecraft", context.getSerializedName());
        return toItemTransform(model.getTransform(location));
    }

    //TODO
    private static @NotNull ItemTransforms toItemTransforms(@NotNull Model model) {
        ItemTransform thirdPersonRight = getDisplayTransform(model, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        ItemTransform thirdPersonLeft = getDisplayTransform(model, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
        ItemTransform firstPersonRight = getDisplayTransform(model, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
        ItemTransform firstPersonLeft = getDisplayTransform(model, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        ItemTransform head = getDisplayTransform(model, ItemDisplayContext.HEAD);
        ItemTransform gui = getDisplayTransform(model, ItemDisplayContext.GUI);
        ItemTransform ground = getDisplayTransform(model, ItemDisplayContext.GROUND);
        ItemTransform fixed = getDisplayTransform(model, ItemDisplayContext.FIXED);

        if (thirdPersonLeft == ItemTransform.NO_TRANSFORM) thirdPersonLeft = thirdPersonRight;
        if (firstPersonLeft == ItemTransform.NO_TRANSFORM) firstPersonLeft = firstPersonRight;

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

}
