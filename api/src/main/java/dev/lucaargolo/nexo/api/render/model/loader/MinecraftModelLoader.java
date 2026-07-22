package dev.lucaargolo.nexo.api.render.model.loader;

import com.google.gson.*;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.render.model.Mesh;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.api.util.Orientation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MinecraftModelLoader implements ModelLoader {

    @Override
    public boolean supports(@NotNull Location path) {
        return path.path().toLowerCase(Locale.ROOT).endsWith(".json");
    }

    @Override
    public @NotNull Model load(@NotNull Nexo nexo, @NotNull Location path, byte @NotNull [] data) {
        JsonObject root = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();

        Model parent = null;
        if (root.has("parent")) {
            Location parentPath = parseResourceLocation(root.get("parent").getAsString());
            String fileName = parentPath.path().substring(parentPath.path().lastIndexOf('/') + 1);
            if (!fileName.contains(".")) parentPath = parentPath.withPathSuffix(".json");
            ModelResource.Minecraft resource = nexo.getResource(Resource.Type.MINECRAFT_MODEL, parentPath);
            parent = resource != null ? resource.model() : null;
        }

        Map<String, Material<?>> materials = new LinkedHashMap<>();
        if (parent != null) materials.putAll(parent.materials());
        parseMaterials(root, materials);

        List<Mesh> meshes;
        if (root.has("elements")) {
            meshes = parseElements(root.getAsJsonArray("elements"), materials);
        } else if (parent != null) {
            meshes = parent.meshes();
        } else {
            meshes = List.of();
        }

        Map<Location, Transform> transforms = new LinkedHashMap<>();
        if (parent != null) transforms.putAll(parent.transforms());
        transforms.putAll(parseDisplay(root));

        boolean shade = root.has("ambientocclusion")
                ? root.get("ambientocclusion").getAsBoolean()
                : parent == null || parent.shade();
        return new Model(meshes, materials, transforms, shade);
    }

    private static void parseMaterials(
            @NotNull JsonObject root,
            @NotNull Map<String, Material<?>> materials
    ) {
        if (!root.has("textures")) return;
        JsonObject textures = root.getAsJsonObject("textures");
        Map<String, String> values = new LinkedHashMap<>();
        for (var entry : textures.entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive()) {
                values.put(entry.getKey(), value.getAsString());
            } else if (value.isJsonObject() && value.getAsJsonObject().has("sprite")) {
                values.put(entry.getKey(), value.getAsJsonObject().get("sprite").getAsString());
            }
        }
        for (String key : values.keySet()) {
            Location texture = resolveTexture(key, values, materials, new ArrayList<>());
            if (texture != null) materials.put(key, new Material<>(texture, texture));
        }
    }

    private static @Nullable Location resolveTexture(
            @NotNull String key,
            @NotNull Map<String, String> values,
            @NotNull Map<String, Material<?>> inherited,
            @NotNull List<String> chain
    ) {
        if (chain.contains(key)) {
            throw new JsonParseException("Cyclic texture reference: " + String.join(" -> ", chain) + " -> " + key);
        }
        String value = values.get(key);
        if (value == null) {
            Material<?> material = inherited.get(key);
            return material == null ? null : material.texture().left();
        }
        if (!value.startsWith("#")) return parseResourceLocation(value);
        chain.add(key);
        Location resolved = resolveTexture(value.substring(1), values, inherited, chain);
        chain.removeLast();
        return resolved;
    }

    private static @NotNull Map<Location, Transform> parseDisplay(@NotNull JsonObject root) {
        // "transforms" is retained for models authored against older Nexo versions.
        JsonObject display = root.has("display")
                ? root.getAsJsonObject("display")
                : root.has("transforms") ? root.getAsJsonObject("transforms") : null;
        if (display == null) return Map.of();

        Map<Location, Transform> transforms = new LinkedHashMap<>();
        for (var entry : display.entrySet()) {
            JsonObject value = entry.getValue().getAsJsonObject();
            transforms.put(Location.of("minecraft", entry.getKey()), new Transform(
                    parseFloat3(value, "rotation", 0),
                    parseFloat3(value, "translation", 0),
                    parseFloat3(value, "scale", 1)
            ));
        }
        return transforms;
    }

    private static @NotNull List<Mesh> parseElements(
            @NotNull JsonArray elements,
            @NotNull Map<String, Material<?>> materials
    ) {
        Map<String, FloatBuilder> geometry = new LinkedHashMap<>();
        int directTextureIndex = 0;
        for (JsonElement element : elements) {
            JsonObject object = element.getAsJsonObject();
            Vector3f from = parseFloat3(object, "from", 0);
            Vector3f to = parseFloat3(object, "to", 0);
            Matrix4f transform = parseRotation(object);
            JsonObject faces = object.getAsJsonObject("faces");
            if (faces == null || faces.isEmpty()) throw new JsonParseException("Element has no faces");

            for (var entry : faces.entrySet()) {
                Orientation orientation;
                try {
                    orientation = Orientation.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new JsonParseException("Unknown face orientation: " + entry.getKey(), e);
                }
                JsonObject face = entry.getValue().getAsJsonObject();
                JsonElement textureElement = face.get("texture");
                if (textureElement == null) {
                    throw new JsonParseException("Face '" + entry.getKey() + "' is missing 'texture'");
                }
                String reference = textureElement.getAsString();
                String material;
                if (reference.startsWith("#")) {
                    material = reference.substring(1);
                } else {
                    Location texture = parseResourceLocation(reference);
                    material = "direct_" + directTextureIndex++;
                    materials.put(material, new Material<>(texture, texture));
                }
                appendFace(geometry.computeIfAbsent(material, ignored -> new FloatBuilder()), from, to, orientation, face, transform);
            }
        }

        List<Mesh> meshes = new ArrayList<>(geometry.size());
        geometry.forEach((material, vertices) -> meshes.add(new Mesh(PrimitiveType.QUADS, material, vertices.toArray())));
        return List.copyOf(meshes);
    }

    private static void appendFace(
            @NotNull FloatBuilder target,
            @NotNull Vector3f from,
            @NotNull Vector3f to,
            @NotNull Orientation orientation,
            @NotNull JsonObject face,
            @NotNull Matrix4f transform
    ) {
        float x0 = from.x / 16.0F;
        float y0 = from.y / 16.0F;
        float z0 = from.z / 16.0F;
        float x1 = to.x / 16.0F;
        float y1 = to.y / 16.0F;
        float z1 = to.z / 16.0F;
        float[][] positions = positions(orientation, x0, y0, z0, x1, y1, z1);
        float[] rectangle = face.has("uv") ? parseFloat4(face.getAsJsonArray("uv")) : defaultUv(from, to, orientation);
        float[][] uv = {
                {rectangle[0], rectangle[1]},
                {rectangle[0], rectangle[3]},
                {rectangle[2], rectangle[3]},
                {rectangle[2], rectangle[1]}
        };
        int turns = Math.floorMod(face.has("rotation") ? face.get("rotation").getAsInt() / 90 : 0, 4);
        Vector3f normal = normal(orientation);
        Matrix3f normalMatrix = new Matrix3f(transform).invert().transpose();
        normalMatrix.transform(normal).normalize();

        for (int i = 0; i < positions.length; i++) {
            Vector3f position = transform.transformPosition(new Vector3f(positions[i][0], positions[i][1], positions[i][2]));
            float[] texture = uv[(i + turns) % 4];
            target.add(
                    position.x, position.y, position.z,
                    1, 1, 1, 1,
                    texture[0] / 16.0F, texture[1] / 16.0F,
                    normal.x, normal.y, normal.z
            );
        }
    }

    private static @NotNull Matrix4f parseRotation(@NotNull JsonObject element) {
        Matrix4f matrix = new Matrix4f();
        if (!element.has("rotation")) return matrix;
        JsonObject rotation = element.getAsJsonObject("rotation");
        Vector3f origin = parseFloat3(rotation, "origin", 8).div(16.0F);
        matrix.translate(origin);
        if (rotation.has("axis") && rotation.has("angle")) {
            float angle = (float) Math.toRadians(rotation.get("angle").getAsFloat());
            String axis = rotation.get("axis").getAsString();
            switch (axis) {
                case "x" -> matrix.rotateX(angle);
                case "y" -> matrix.rotateY(angle);
                case "z" -> matrix.rotateZ(angle);
                default -> throw new JsonParseException("Unsupported model rotation axis: " + axis);
            }
            if (rotation.has("rescale") && rotation.get("rescale").getAsBoolean()) {
                float scale = rescale(rotation.get("angle").getAsFloat());
                switch (axis) {
                    case "x" -> matrix.scale(1, scale, scale);
                    case "y" -> matrix.scale(scale, 1, scale);
                    case "z" -> matrix.scale(scale, scale, 1);
                }
            }
        } else {
            if (rotation.has("x")) matrix.rotateX((float) Math.toRadians(rotation.get("x").getAsFloat()));
            if (rotation.has("y")) matrix.rotateY((float) Math.toRadians(rotation.get("y").getAsFloat()));
            if (rotation.has("z")) matrix.rotateZ((float) Math.toRadians(rotation.get("z").getAsFloat()));
        }
        return matrix.translate(-origin.x, -origin.y, -origin.z);
    }

    private static float rescale(float angle) {
        float cosine = Math.abs((float) Math.cos(Math.toRadians(angle)));
        return cosine > 1.0E-4F ? 1.0F / cosine : 1.0F;
    }

    public static float @NotNull [] boxVertices(float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
        Vector3f from = new Vector3f(fromX, fromY, fromZ);
        Vector3f to = new Vector3f(toX, toY, toZ);
        FloatBuilder vertices = new FloatBuilder();
        for (Orientation orientation : Orientation.values()) {
            appendFace(vertices, from, to, orientation, new JsonObject(), new Matrix4f());
        }
        return vertices.toArray();
    }

    private static float @NotNull [] @NotNull [] positions(
            @NotNull Orientation orientation,
            float x0, float y0, float z0,
            float x1, float y1, float z1
    ) {
        return switch (orientation) {
            case UP -> new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
            case DOWN -> new float[][]{{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}};
            case NORTH -> new float[][]{{x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}};
            case SOUTH -> new float[][]{{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}};
            case WEST -> new float[][]{{x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}};
            case EAST -> new float[][]{{x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}};
        };
    }

    private static float @NotNull [] defaultUv(
            @NotNull Vector3f from,
            @NotNull Vector3f to,
            @NotNull Orientation orientation
    ) {
        return switch (orientation) {
            case DOWN -> new float[]{from.x, 16 - to.z, to.x, 16 - from.z};
            case UP -> new float[]{from.x, from.z, to.x, to.z};
            case NORTH -> new float[]{16 - to.x, 16 - to.y, 16 - from.x, 16 - from.y};
            case SOUTH -> new float[]{from.x, 16 - to.y, to.x, 16 - from.y};
            case WEST -> new float[]{from.z, 16 - to.y, to.z, 16 - from.y};
            case EAST -> new float[]{16 - to.z, 16 - to.y, 16 - from.z, 16 - from.y};
        };
    }

    private static @NotNull Vector3f normal(@NotNull Orientation orientation) {
        return switch (orientation) {
            case UP -> new Vector3f(0, 1, 0);
            case DOWN -> new Vector3f(0, -1, 0);
            case NORTH -> new Vector3f(0, 0, -1);
            case SOUTH -> new Vector3f(0, 0, 1);
            case WEST -> new Vector3f(-1, 0, 0);
            case EAST -> new Vector3f(1, 0, 0);
        };
    }

    private static @NotNull Vector3f parseFloat3(@NotNull JsonObject object, @NotNull String key, float defaultValue) {
        if (!object.has(key)) return new Vector3f(defaultValue);
        JsonArray values = object.getAsJsonArray(key);
        if (values.size() != 3) throw new JsonParseException("Expected 3 values for '" + key + "'");
        return new Vector3f(values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat());
    }

    private static float @NotNull [] parseFloat4(@NotNull JsonArray values) {
        if (values.size() != 4) throw new JsonParseException("Expected 4 UV values");
        return new float[]{
                values.get(0).getAsFloat(), values.get(1).getAsFloat(),
                values.get(2).getAsFloat(), values.get(3).getAsFloat()
        };
    }

    static @NotNull Location parseResourceLocation(@NotNull String value) {
        int colon = value.indexOf(':');
        return colon < 0
                ? Location.of("minecraft", value)
                : Location.of(value.substring(0, colon), value.substring(colon + 1));
    }

}
