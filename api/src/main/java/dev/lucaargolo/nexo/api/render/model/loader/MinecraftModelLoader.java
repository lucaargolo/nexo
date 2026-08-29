package dev.lucaargolo.nexo.api.render.model.loader;

import com.google.gson.*;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.render.model.Mesh;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.render.util.FloatBuilder;
import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
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
    public @NotNull List<@NotNull String> extensions() {
        return List.of(".json");
    }

    @Override
    public @NotNull Model load(@NotNull Nexo nexo, @NotNull Location path, byte @NotNull [] data) {
        JsonObject root = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
        List<JsonObject> chain = loadParentChain(nexo, path, root);

        Map<String, Material<?>> materials = new LinkedHashMap<>();
        parseMaterials(chain, materials);

        List<Mesh> meshes = List.of();
        for (JsonObject model : chain) {
            if (model.has("elements")) {
                JsonArray elements = model.getAsJsonArray("elements");
                Map<String, FloatBuilder> geometry = new LinkedHashMap<>();
                int directTextureIndex = 0;
                for (JsonElement element : elements) {
                    JsonObject object = element.getAsJsonObject();
                    Vector3f from = parseFloat3(object, "from", 0);
                    Vector3f to = parseFloat3(object, "to", 0);
                    Matrix4f transform = parseRotation(object);
                    JsonObject faces = object.getAsJsonObject("faces");
                    if (faces == null || faces.isEmpty()) {
                        throw new JsonParseException("Element has no faces");
                    }
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
                            materials.put(material, Material.of(texture));
                        }
                        appendFace(geometry.computeIfAbsent(material, ignored -> new FloatBuilder()), from, to, orientation, face, transform);
                    }
                }
                List<Mesh> loadedMeshes = new ArrayList<>(geometry.size());
                for (var entry : geometry.entrySet()) {
                    loadedMeshes.add(new Mesh(PrimitiveType.QUADS, entry.getKey(), entry.getValue().toArray()));
                }
                meshes = List.copyOf(loadedMeshes);
                break;
            }
        }

        Map<Location, Transform> transforms = new LinkedHashMap<>();
        for (int i = chain.size() - 1; i >= 0; i--) {
            JsonObject display = chain.get(i).has("display")
                    ? chain.get(i).getAsJsonObject("display")
                    : chain.get(i).has("transforms") ? chain.get(i).getAsJsonObject("transforms") : null;
            if (display != null) {
                for (var entry : display.entrySet()) {
                    JsonObject value = entry.getValue().getAsJsonObject();
                    transforms.put(Location.of("minecraft", entry.getKey()), new Transform(
                            parseFloat3(value, "rotation", 0),
                            parseFloat3(value, "translation", 0),
                            parseFloat3(value, "scale", 1)
                    ));
                }
            }
        }

        boolean shade = true;
        for (JsonObject model : chain) {
            if (model.has("ambientocclusion")) {
                shade = model.get("ambientocclusion").getAsBoolean();
                break;
            }
        }
        return new Model(data, meshes, materials, transforms, shade);
    }

    private static @NotNull List<JsonObject> loadParentChain(
            @NotNull Nexo nexo,
            @NotNull Location path,
            @NotNull JsonObject root
    ) {
        List<JsonObject> chain = new ArrayList<>();
        Set<Location> visited = new LinkedHashSet<>();
        visited.add(normalizeModelPath(path));
        chain.add(root);

        JsonObject current = root;
        while (current.has("parent")) {
            Location parentPath = normalizeModelPath(parseResourceLocation(current.get("parent").getAsString()));
            if (!visited.add(parentPath)) {
                throw new JsonParseException("Cyclic model parent chain: " + String.join(" -> ", visited.stream().map(Location::toString).toList()) + " -> " + parentPath);
            }
            byte[] parentData = nexo.loadResource(parentPath);
            if (parentData == null) {
                break;
            }
            current = JsonParser.parseString(new String(parentData, StandardCharsets.UTF_8)).getAsJsonObject();
            chain.add(current);
        }
        return chain;
    }

    private static @NotNull Location normalizeModelPath(@NotNull Location path) {
        String value = path.path();
        if (!value.startsWith("models/")) {
            value = "models/" + value;
        }
        if (!value.endsWith(".json")) {
            value += ".json";
        }
        return Location.of(path.namespace(), value);
    }

    private static void parseMaterials(
            @NotNull List<JsonObject> chain,
            @NotNull Map<String, Material<?>> materials
    ) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = chain.size() - 1; i >= 0; i--) {
            JsonObject root = chain.get(i);
            if (!root.has("textures")) {
                continue;
            }
            for (var entry : root.getAsJsonObject("textures").entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonPrimitive()) {
                    values.put(entry.getKey(), value.getAsString());
                } else if (value.isJsonObject() && value.getAsJsonObject().has("sprite")) {
                    values.put(entry.getKey(), value.getAsJsonObject().get("sprite").getAsString());
                }
            }
        }
        for (String key : values.keySet()) {
            Location texture = resolveTexture(key, values, new ArrayList<>());
            if (texture != null) {
                materials.put(key, Material.of(texture));
            }
        }
    }

    private static @Nullable Location resolveTexture(
            @NotNull String key,
            @NotNull Map<String, String> values,
            @NotNull List<String> chain
    ) {
        if (chain.contains(key)) {
            throw new JsonParseException("Cyclic texture reference: " + String.join(" -> ", chain) + " -> " + key);
        }
        String value = values.get(key);
        if (value == null) {
            return null;
        }
        if (!value.startsWith("#")) {
            return parseResourceLocation(value);
        }
        chain.add(key);
        Location resolved = resolveTexture(value.substring(1), values, chain);
        chain.removeLast();
        return resolved;
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
        float[][] positions = switch (orientation) {
            case UP -> new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
            case DOWN -> new float[][]{{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}};
            case NORTH -> new float[][]{{x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}};
            case SOUTH -> new float[][]{{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}};
            case WEST -> new float[][]{{x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}};
            case EAST -> new float[][]{{x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}};
        };
        float[] rectangle;
        if (face.has("uv")) {
            JsonArray values = face.getAsJsonArray("uv");
            if (values.size() != 4) {
                throw new JsonParseException("Expected 4 UV values");
            }
            rectangle = new float[]{
                    values.get(0).getAsFloat(), values.get(1).getAsFloat(),
                    values.get(2).getAsFloat(), values.get(3).getAsFloat()
            };
        } else {
            rectangle = switch (orientation) {
                case DOWN -> new float[]{from.x, 16 - to.z, to.x, 16 - from.z};
                case UP -> new float[]{from.x, from.z, to.x, to.z};
                case NORTH -> new float[]{16 - to.x, 16 - to.y, 16 - from.x, 16 - from.y};
                case SOUTH -> new float[]{from.x, 16 - to.y, to.x, 16 - from.y};
                case WEST -> new float[]{from.z, 16 - to.y, to.z, 16 - from.y};
                case EAST -> new float[]{16 - to.z, 16 - to.y, 16 - from.z, 16 - from.y};
            };
        }
        float[][] uv = {
                {rectangle[0], rectangle[1]},
                {rectangle[0], rectangle[3]},
                {rectangle[2], rectangle[3]},
                {rectangle[2], rectangle[1]}
        };
        int turns = Math.floorMod(face.has("rotation") ? face.get("rotation").getAsInt() / 90 : 0, 4);
        Vector3f normal = switch (orientation) {
            case UP -> new Vector3f(0, 1, 0);
            case DOWN -> new Vector3f(0, -1, 0);
            case NORTH -> new Vector3f(0, 0, -1);
            case SOUTH -> new Vector3f(0, 0, 1);
            case WEST -> new Vector3f(-1, 0, 0);
            case EAST -> new Vector3f(1, 0, 0);
        };
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
        if (!element.has("rotation")) {
            return matrix;
        }
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
                float cosine = Math.abs((float) Math.cos(Math.toRadians(rotation.get("angle").getAsFloat())));
                float scale = cosine > 1.0E-4F ? 1.0F / cosine : 1.0F;
                switch (axis) {
                    case "x" -> matrix.scale(1, scale, scale);
                    case "y" -> matrix.scale(scale, 1, scale);
                    case "z" -> matrix.scale(scale, scale, 1);
                }
            }
        } else {
            if (rotation.has("x")) {
                matrix.rotateX((float) Math.toRadians(rotation.get("x").getAsFloat()));
            }
            if (rotation.has("y")) {
                matrix.rotateY((float) Math.toRadians(rotation.get("y").getAsFloat()));
            }
            if (rotation.has("z")) {
                matrix.rotateZ((float) Math.toRadians(rotation.get("z").getAsFloat()));
            }
        }
        return matrix.translate(-origin.x, -origin.y, -origin.z);
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

    private static @NotNull Vector3f parseFloat3(@NotNull JsonObject object, @NotNull String key, float defaultValue) {
        if (!object.has(key)) {
            return new Vector3f(defaultValue);
        }
        JsonArray values = object.getAsJsonArray(key);
        if (values.size() != 3) {
            throw new JsonParseException("Expected 3 values for '" + key + "'");
        }
        return new Vector3f(values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat());
    }

    static @NotNull Location parseResourceLocation(@NotNull String value) {
        int colon = value.indexOf(':');
        return colon < 0
                ? Location.of("minecraft", value)
                : Location.of(value.substring(0, colon), value.substring(colon + 1));
    }

}
