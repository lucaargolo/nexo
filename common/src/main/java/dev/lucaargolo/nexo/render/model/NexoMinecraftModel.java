package dev.lucaargolo.nexo.render.model;

import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.render.model.ModelRenderer;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.render.MinecraftBakedGraphics3D;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class NexoMinecraftModel<U> implements UnbakedModel {

    private final StaticRenderer<Graphics3D, U> renderer;
    private final Set<Location> textures;

    public NexoMinecraftModel(
            @NotNull StaticRenderer<Graphics3D, U> renderer
    ) {
        this.renderer = renderer;
        this.textures = MinecraftBakedGraphics3D.textures(renderer);
    }

    public @NotNull Set<Location> textures() {
        return textures;
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
        MinecraftBakedGraphics3D graphics = MinecraftBakedGraphics3D.bake(
                renderer,
                textureGetter,
                modelState.getRotation().getMatrix()
        );

        TextureAtlasSprite particle = graphics.particle();
        if (particle == null) {
            particle = textureGetter.apply(new Material(InventoryMenu.BLOCK_ATLAS, MissingTextureAtlasSprite.getLocation()));
        }

        return new RendererBakedModel(
                graphics.quads(),
                ambientOcclusion(),
                itemTransforms(),
                particle
        );
    }

    private boolean ambientOcclusion() {
        if (renderer instanceof ModelRenderer modelRenderer) {
            return modelRenderer.model().shade();
        }
        return true;
    }

    private @NotNull ItemTransforms itemTransforms() {
        if (renderer instanceof ModelRenderer modelRenderer) {
            return toItemTransforms(modelRenderer.model());
        }
        return ItemTransforms.NO_TRANSFORMS;
    }

    private static @NotNull ItemTransform toItemTransform(@Nullable Model.Transform transform) {
        if (transform == null) return ItemTransform.NO_TRANSFORM;
        Vector3f translation = new Vector3f(transform.translation()).mul(0.0625F);
        return new ItemTransform(
                new Vector3f(transform.rotation()),
                translation,
                new Vector3f(transform.scale())
        );
    }

    private static @NotNull ItemTransform getDisplayTransform(
            @NotNull Model model,
            @NotNull ItemDisplayContext context
    ) {
        Location location = Location.of("minecraft", context.getSerializedName());
        return toItemTransform(model.getTransform(location));
    }

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

    private static final class RendererBakedModel implements BakedModel {

        private final @NotNull List<BakedQuad> quads;
        private final boolean ambientOcclusion;
        private final @NotNull ItemTransforms transforms;
        private final @NotNull TextureAtlasSprite particle;

        private RendererBakedModel(
                @NotNull List<BakedQuad> quads,
                boolean ambientOcclusion,
                @NotNull ItemTransforms transforms,
                @NotNull TextureAtlasSprite particle
        ) {
            this.quads = quads;
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
            return side == null ? quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return ambientOcclusion;
        }

        @Override
        public boolean isGui3d() {
            return true;
        }

        @Override
        public boolean usesBlockLight() {
            return true;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public @NotNull TextureAtlasSprite getParticleIcon() {
            return particle;
        }

        @Override
        public @NotNull ItemTransforms getTransforms() {
            return transforms;
        }

        @Override
        public @NotNull ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }

}
