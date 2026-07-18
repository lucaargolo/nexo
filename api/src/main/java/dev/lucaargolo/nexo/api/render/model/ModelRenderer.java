package dev.lucaargolo.nexo.api.render.model;

import dev.lucaargolo.nexo.api.render.DrawCall;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
import dev.lucaargolo.nexo.api.render.util.VertexFormat;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.api.util.Orientation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

public final class ModelRenderer<U> extends StaticRenderer<Graphics3D, U> {

    public static final @NotNull Location MISSING_TEXTURE = Location.of("nexo", "null");

    private final @NotNull Model model;
    private final @NotNull List<DrawCall<Graphics3D>> calls;
    private final @NotNull Map<String, Location> textures;

    public ModelRenderer(@NotNull Model model) {
        this.model = model;
        CompiledModel compiled = compile(model);
        this.calls = compiled.calls();
        this.textures = compiled.textures();
    }

    public @NotNull Model model() {
        return model;
    }

    @Override
    public @NotNull List<@NotNull DrawCall<Graphics3D>> calls(@NotNull U unit) {
        return calls;
    }

    @Override
    public @NotNull Map<String, Location> textures() {
        return textures;
    }

    @Override
    public @NotNull Transform transform(@NotNull Location location) {
        Transform transform = model.getTransform(location);
        if (transform != null) return transform;
        return new Transform(new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));
    }

    @Override
    public boolean shaded() {
        return model.shade();
    }

    private static @NotNull CompiledModel compile(@NotNull Model model) {
        List<DrawCall<Graphics3D>> calls = new ArrayList<>();
        Map<String, Location> textures = new LinkedHashMap<>();
        for (Cube cube : model.cubes()) {
            List<FaceGeometry> faces = new ArrayList<>(cube.faces().size());
            for (var entry : cube.faces().entrySet()) {
                Face face = entry.getValue();
                Location texture = resolveTexture(model, face);
                String textureKey = face.texture();
                if (textureKey.startsWith("#")) textureKey = textureKey.substring(1);
                textures.put(textureKey, texture);
                faces.add(new FaceGeometry(
                        texture,
                        vertices(cube, entry.getKey(), face),
                        normal(entry.getKey())
                ));
            }
            calls.add(graphics -> renderCube(graphics, cube.rotation(), faces));
        }
        return new CompiledModel(List.copyOf(calls), Collections.unmodifiableMap(textures));
    }

    private static void renderCube(
            @NotNull Graphics3D graphics,
            @Nullable Cube.Rotation rotation,
            @NotNull List<FaceGeometry> faces
    ) {
        graphics.pushMatrix();
        if (rotation != null) {
            applyRotation(graphics, rotation);
        }
        for (FaceGeometry face : faces) {
            graphics.bindTexture(face.texture());
            graphics.begin(PrimitiveType.QUADS, VertexFormat.POSITION_TEX_NORMAL);
            for (float[] vertex : face.vertices()) {
                graphics.vertex(
                        vertex[0], vertex[1], vertex[2],
                        vertex[3], vertex[4],
                        face.normal()[0], face.normal()[1], face.normal()[2]
                );
            }
            graphics.end();
        }
        graphics.popMatrix();
    }

    private static void applyRotation(@NotNull Graphics3D graphics, @NotNull Cube.Rotation rotation) {
        float x = rotation.origin().x() / 16f;
        float y = rotation.origin().y() / 16f;
        float z = rotation.origin().z() / 16f;
        graphics.translate(x, y, z);

        if (rotation.axis() != null) {
            switch (rotation.axis()) {
                case "x" -> graphics.rotate(rotation.angle(), 1.0F, 0.0F, 0.0F);
                case "y" -> graphics.rotate(rotation.angle(), 0.0F, 1.0F, 0.0F);
                case "z" -> graphics.rotate(rotation.angle(), 0.0F, 0.0F, 1.0F);
                default -> throw new IllegalArgumentException("Unsupported model rotation axis: " + rotation.axis());
            }
            if (rotation.rescale()) {
                float scale = rescale(rotation.angle());
                switch (rotation.axis()) {
                    case "x" -> graphics.scale(1.0F, scale, scale);
                    case "y" -> graphics.scale(scale, 1.0F, scale);
                    case "z" -> graphics.scale(scale, scale, 1.0F);
                }
            }
        } else {
            if (rotation.x() != null) graphics.rotate(rotation.x(), 1.0F, 0.0F, 0.0F);
            if (rotation.y() != null) graphics.rotate(rotation.y(), 0.0F, 1.0F, 0.0F);
            if (rotation.z() != null) graphics.rotate(rotation.z(), 0.0F, 0.0F, 1.0F);
        }

        graphics.translate(-x, -y, -z);
    }

    private static float rescale(float angle) {
        float cosine = Math.abs((float) Math.cos(Math.toRadians(angle)));
        return cosine > 1.0E-4F ? 1.0F / cosine : 1.0F;
    }

    private static @NotNull Location resolveTexture(@NotNull Model model, @NotNull Face face) {
        String key = face.texture();
        if (key.startsWith("#")) key = key.substring(1);
        return model.textures().getOrDefault(key, MISSING_TEXTURE);
    }

    private static float @NotNull [] @NotNull [] vertices(
            @NotNull Cube cube,
            @NotNull Orientation orientation,
            @NotNull Face face
    ) {
        float x0 = cube.fromX() / 16f;
        float y0 = cube.fromY() / 16f;
        float z0 = cube.fromZ() / 16f;
        float x1 = cube.toX() / 16f;
        float y1 = cube.toY() / 16f;
        float z1 = cube.toZ() / 16f;

        float[][] positions = switch (orientation) {
            case UP -> new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
            case DOWN -> new float[][]{{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}};
            case NORTH -> new float[][]{{x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}};
            case SOUTH -> new float[][]{{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}};
            case WEST -> new float[][]{{x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}};
            case EAST -> new float[][]{{x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}};
        };

        float[] rectangle = face.uv() != null ? face.uv() : defaultUv(cube, orientation);
        float[][] uv = {
                {rectangle[0], rectangle[1]},
                {rectangle[0], rectangle[3]},
                {rectangle[2], rectangle[3]},
                {rectangle[2], rectangle[1]}
        };
        int turns = Math.floorMod(face.rotation() / 90, 4);

        float[][] result = new float[4][5];
        for (int i = 0; i < result.length; i++) {
            System.arraycopy(positions[i], 0, result[i], 0, 3);
            float[] rotatedUv = uv[(i + turns) % 4];
            result[i][3] = rotatedUv[0] / 16.0F;
            result[i][4] = rotatedUv[1] / 16.0F;
        }
        return result;
    }

    private static float @NotNull [] defaultUv(@NotNull Cube cube, @NotNull Orientation orientation) {
        return switch (orientation) {
            case DOWN -> new float[]{cube.fromX(), 16.0F - cube.toZ(), cube.toX(), 16.0F - cube.fromZ()};
            case UP -> new float[]{cube.fromX(), cube.fromZ(), cube.toX(), cube.toZ()};
            case NORTH -> new float[]{16.0F - cube.toX(), 16.0F - cube.toY(), 16.0F - cube.fromX(), 16.0F - cube.fromY()};
            case SOUTH -> new float[]{cube.fromX(), 16.0F - cube.toY(), cube.toX(), 16.0F - cube.fromY()};
            case WEST -> new float[]{cube.fromZ(), 16.0F - cube.toY(), cube.toZ(), 16.0F - cube.fromY()};
            case EAST -> new float[]{16.0F - cube.toZ(), 16.0F - cube.toY(), 16.0F - cube.fromZ(), 16.0F - cube.fromY()};
        };
    }

    private static float @NotNull [] normal(@NotNull Orientation orientation) {
        return switch (orientation) {
            case UP -> new float[]{0.0F, 1.0F, 0.0F};
            case DOWN -> new float[]{0.0F, -1.0F, 0.0F};
            case NORTH -> new float[]{0.0F, 0.0F, -1.0F};
            case SOUTH -> new float[]{0.0F, 0.0F, 1.0F};
            case WEST -> new float[]{-1.0F, 0.0F, 0.0F};
            case EAST -> new float[]{1.0F, 0.0F, 0.0F};
        };
    }

    private record CompiledModel(
            @NotNull List<DrawCall<Graphics3D>> calls,
            @NotNull Map<String, Location> textures
    ) {
    }

    private record FaceGeometry(
            @NotNull Location texture,
            float @NotNull [] @NotNull [] vertices,
            float @NotNull [] normal
    ) {
    }

}
