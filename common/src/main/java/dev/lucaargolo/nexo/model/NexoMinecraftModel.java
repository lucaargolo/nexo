package dev.lucaargolo.nexo.model;

import com.mojang.datafixers.util.Either;
import dev.lucaargolo.nexo.api.model.Cube;
import dev.lucaargolo.nexo.api.model.Face;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Orientation;
import dev.lucaargolo.nexo.mixed.BlockElementRotationMixed;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Function;

public class NexoMinecraftModel implements UnbakedModel {

    private final Model model;

    public NexoMinecraftModel(Model model) {
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

        Map<String, Either<Material, String>> textureMap = new HashMap<>();
        for (var entry : model.textures().entrySet()) {
            String texPath = entry.getValue().path();
            int dot = texPath.lastIndexOf('.');
            if (dot > -1) {
                texPath = texPath.substring(0, dot);
            }
            ResourceLocation texLoc = ResourceLocation.fromNamespaceAndPath(entry.getValue().namespace(), texPath);
            Material material = new Material(InventoryMenu.BLOCK_ATLAS, texLoc);
            textureMap.put(entry.getKey(), Either.left(material));
        }

        // Add #-references from face textures not already in the map
        // (MinecraftModelLoader skips #-prefixed entries since they aren't Locations)
        for (Cube cube : model.cubes()) {
            for (Face face : cube.faces().values()) {
                String tex = face.texture();
                if (tex.startsWith("#")) {
                    String varName = tex.substring(1);
                    if (!textureMap.containsKey(varName)) {
                        textureMap.put(varName, Either.right(tex));
                    }
                }
            }
        }

        // Convert Nexo Cubes to Minecraft BlockElements
        List<BlockElement> elements = new ArrayList<>();
        for (Cube cube : model.cubes()) {
            elements.add(toBlockElement(cube));
        }

        // Build a BlockModel directly — no parent needed
        BlockModel blockModel = new BlockModel(
            null,
            elements,
            textureMap,
            model.shade(),
            BlockModel.GuiLight.SIDE,
            ItemTransforms.NO_TRANSFORMS,
            List.of()
        );

        // Resolve parent references (required even with null parent for setup)
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
                face.texture().startsWith("#") ? face.texture() : "#" + face.texture(),
                uv
            );
            faces.put(mcDir, mcFace);
        }

        // Convert Cube.Rotation → BlockElementRotation
        BlockElementRotation mcRotation = null;
        Cube.Rotation r = cube.rotation();
        if (r != null) {
            Vector3f origin = r.origin();
            if (r.axis() != null) {
                // Format 1 & 2: single-axis
                Direction.Axis axis = Direction.Axis.byName(r.axis());
                mcRotation = new BlockElementRotation(origin, axis, r.angle(), r.rescale());
            } else if (r.x() != null || r.y() != null || r.z() != null) {
                // Format 3: multi-axis — store euler angles via mixin
                mcRotation = new BlockElementRotation(origin, Direction.Axis.Y, 0.0F, r.rescale());
                ((BlockElementRotationMixed) (Object) mcRotation).nexo$setEulerRotation(
                    new Vector3f(
                        r.x() != null ? r.x() : 0.0F,
                        r.y() != null ? r.y() : 0.0F,
                        r.z() != null ? r.z() : 0.0F
                    )
                );
            }
        }

        return new BlockElement(from, to, faces, mcRotation, cube.shade());
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
