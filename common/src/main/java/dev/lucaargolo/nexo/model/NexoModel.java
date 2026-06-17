package dev.lucaargolo.nexo.model;

import com.mojang.datafixers.util.Either;
import dev.lucaargolo.nexo.api.model.Cube;
import dev.lucaargolo.nexo.api.model.Face;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Orientation;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Function;

public class NexoModel implements UnbakedModel {

    private final Model model;

    public NexoModel(Model model) {
        this.model = model;
    }

    @Override
    public @NotNull Collection<ResourceLocation> getDependencies() {
        return List.of();
    }

    @Override
    public void resolveParents(@NotNull Function<ResourceLocation, UnbakedModel> resolver) {
    }

    @Override
    public BakedModel bake(@NotNull ModelBaker baker, @NotNull Function<Material, TextureAtlasSprite> textureGetter, @NotNull ModelState modelState) {

        // Texture map: variable name -> unresolved texture string (e.g. "nexo:block/stone")
        Map<String, Either<Material, String>> textureMap = new HashMap<>();
        for (var entry : model.textures().entrySet()) {
            String ref = entry.getValue().namespace() + ":" + entry.getValue().path();
            textureMap.put(entry.getKey(), Either.right(ref));
        }

        // Convert Nexo Cubes to Minecraft BlockElements
        List<BlockElement> elements = new ArrayList<>();
        for (Cube cube : model.cubes()) {
            elements.add(toBlockElement(cube));
        }

        // Build a BlockModel directly — skip JSON entirely
        BlockModel blockModel = new BlockModel(
            null, // no parent — first param is parent location, not model location
            elements,
            textureMap,
            model.ambientOcclusion(),
            BlockModel.GuiLight.SIDE,
            ItemTransforms.NO_TRANSFORMS,
            List.of()
        );

        // Resolve internal texture references (converts Either.right strings to Either.left Materials)
        blockModel.resolveParents(baker::getModel);

        // Delegate baking to vanilla — handles face baking, quad generation, etc.
        return blockModel.bake(baker, textureGetter, modelState);
    }

    // ---- Nexo -> Minecraft conversion ----

    private static BlockElement toBlockElement(Cube cube) {
        Vector3f from = new Vector3f(cube.fromX(), cube.fromY(), cube.fromZ());
        Vector3f to = new Vector3f(cube.toX(), cube.toY(), cube.toZ());

        Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
        for (var faceEntry : cube.faces().entrySet()) {
            Direction mcDir = direction(faceEntry.getKey());
            Face face = faceEntry.getValue();

            BlockFaceUV uv = face.uv() != null
                ? new BlockFaceUV(face.uv(), face.rotation())
                : new BlockFaceUV(null, face.rotation());

            Direction cullDir = face.cullFace() != null
                ? direction(face.cullFace())
                : null;

            BlockElementFace mcFace = new BlockElementFace(
                cullDir,
                face.tintIndex(),
                "#" + face.texture(),
                uv
            );
            faces.put(mcDir, mcFace);
        }

        return new BlockElement(from, to, faces, null, true);
    }

    private static Direction direction(Orientation orientation) {
        return switch (orientation) {
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case EAST -> Direction.EAST;
            case WEST -> Direction.WEST;
        };
    }
}
