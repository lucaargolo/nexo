package dev.lucaargolo.nexo.api.model.loader;

import com.google.gson.*;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.model.Cube;
import dev.lucaargolo.nexo.api.model.Face;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.api.util.Orientation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class MinecraftModelLoader extends ModelLoader {

    @Override
    public @Nullable Model tryLoad(@NotNull Nexo nexo, @NotNull Location path, byte @NotNull [] data) {
        if (!path.path().toLowerCase(Locale.ROOT).endsWith(".json")) {
            return null;
        }

        JsonObject root;
        try {
            root = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            return null;
        }

        if (!root.has("parent") && !root.has("elements") && !root.has("textures")) {
            return null;
        }

        // Recursively resolve parent model
        Model parentModel = null;
        if (root.has("parent")) {
            String parentStr = root.get("parent").getAsString();
            Location parentLoc = parseResourceLocation(parentStr);
            parentModel = nexo.getModel(parentLoc);
        }

        // Merge textures: parent base, child overrides
        Map<String, Location> textures = new HashMap<>();
        if (parentModel != null) {
            textures.putAll(parentModel.textures());
        }
        textures.putAll(parseTextures(root));

        // Merge elements: parent first, then child
        List<Cube> cubes = new ArrayList<>();
        if (parentModel != null) {
            cubes.addAll(parentModel.cubes());
        }
        cubes.addAll(parseElements(root));

        // Merge transforms: parent base, child overrides
        Map<Location, Model.Transform> transforms = new HashMap<>();
        if (parentModel != null) {
            transforms.putAll(parentModel.transforms());
        }
        transforms.putAll(parseDisplay(root));

        boolean shade;
        if (root.has("ambientocclusion")) {
            shade = root.get("ambientocclusion").getAsBoolean();
        } else if (parentModel != null) {
            shade = parentModel.shade();
        } else {
            shade = true;
        }

        return new Model(cubes, textures, transforms, shade);
    }

    private static Map<String, Location> parseTextures(JsonObject root) {
        Map<String, Location> textures = new HashMap<>();
        if (!root.has("textures")) return textures;

        JsonObject texObj = root.getAsJsonObject("textures");
        for (var entry : texObj.entrySet()) {
            Location loc = parseTextureValue(entry.getValue());
            if (loc != null) {
                textures.put(entry.getKey(), loc);
            }
        }
        return textures;
    }

    private static @Nullable Location parseTextureValue(JsonElement element) {
        if (element.isJsonPrimitive()) {
            String val = element.getAsString();
            if (val.startsWith("#")) return null;
            return parseResourceLocation(val);
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("sprite")) {
                return parseResourceLocation(obj.get("sprite").getAsString());
            }
            return null;
        }
        return null;
    }

    private static Location parseResourceLocation(String val) {
        int colon = val.indexOf(':');
        if (colon < 0) {
            return Location.of("minecraft", val);
        }
        return Location.of(val.substring(0, colon), val.substring(colon + 1));
    }

    private static Map<Location, Model.Transform> parseDisplay(JsonObject root) {
        if (!root.has("transforms"))
            return Map.of();

        JsonObject displayObj = root.getAsJsonObject("transforms");
        Map<Location, Model.Transform> display = new HashMap<>();

        for (var entry : displayObj.entrySet()) {
            Location loc = Location.of("minecraft", entry.getKey());
            JsonObject transformObj = entry.getValue().getAsJsonObject();
            display.put(loc, parseTransform(transformObj));
        }

        return display;
    }

    private static Model.Transform parseTransform(JsonObject obj) {
        Vector3f rotation = parseFloat3(obj, "rotation", 0);
        Vector3f translation = parseFloat3(obj, "translation", 0);
        Vector3f scale = parseFloat3(obj, "scale", 1);
        return new Model.Transform(rotation, translation, scale);
    }

    private static Vector3f parseFloat3(JsonObject obj, String key, float defaultValue) {
        if (!obj.has(key)) return new Vector3f(defaultValue, defaultValue, defaultValue);
        JsonArray arr = obj.getAsJsonArray(key);
        if (arr.size() != 3) {
            throw new JsonParseException("Expected 3 values for '" + key + "', found: " + arr.size());
        }
        return new Vector3f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
    }

    private static List<Cube> parseElements(JsonObject root) {
        if (!root.has("elements")) return List.of();

        JsonArray elementsArr = root.getAsJsonArray("elements");
        List<Cube> cubes = new ArrayList<>(elementsArr.size());

        for (JsonElement el : elementsArr) {
            cubes.add(parseElement(el.getAsJsonObject()));
        }
        return cubes;
    }

    private static Cube parseElement(JsonObject obj) {
        Vector3f from = parseFloat3(obj, "from", 0);
        Vector3f to = parseFloat3(obj, "to", 0);

        Cube.Rotation rotation = parseRotation(obj);

        boolean shade = true;
        if (obj.has("shade")) {
            shade = obj.get("shade").getAsBoolean();
        }

        boolean emissive = false;
        if (obj.has("light_emission")) {
            emissive = obj.get("light_emission").getAsInt() > 0;
        }

        Map<Orientation, Face> faces = parseFaces(obj);

        return new Cube(
            from.x(), from.y(), from.z(),
            to.x(), to.y(), to.z(),
            faces, rotation, shade, emissive
        );
    }

    private static @Nullable Cube.Rotation parseRotation(JsonObject obj) {
        if (!obj.has("rotation")) return null;

        JsonObject rotObj = obj.getAsJsonObject("rotation");
        Vector3f origin = parseFloat3(rotObj, "origin", 0);

        boolean rescale = false;
        if (rotObj.has("rescale")) {
            rescale = rotObj.get("rescale").getAsBoolean();
        }

        // Format 3: x / y / z independent axes — construct directly to preserve nullable Floats
        if (rotObj.has("x") || rotObj.has("y") || rotObj.has("z")) {
            Float x = rotObj.has("x") ? rotObj.get("x").getAsFloat() : null;
            Float y = rotObj.has("y") ? rotObj.get("y").getAsFloat() : null;
            Float z = rotObj.has("z") ? rotObj.get("z").getAsFloat() : null;
            return new Cube.Rotation(origin, null, 0, x, y, z, rescale);
        }

        // Format 1 & 2: axis + angle
        if (!rotObj.has("axis") || !rotObj.has("angle")) {
            return null;
        }
        String axis = rotObj.get("axis").getAsString();
        float angle = rotObj.get("angle").getAsFloat();
        return Cube.Rotation.axisAngle(origin, axis, angle, rescale);
    }

    private static Map<Orientation, Face> parseFaces(JsonObject obj) {
        JsonObject facesObj = obj.getAsJsonObject("faces");
        Map<Orientation, Face> faces = new HashMap<>();

        for (var entry : facesObj.entrySet()) {
            Orientation orientation = Orientation.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
            JsonObject faceObj = entry.getValue().getAsJsonObject();

            String texture = faceObj.get("texture").getAsString();

            Orientation cullFace = null;
            if (faceObj.has("cullface")) {
                try {
                    cullFace = Orientation.valueOf(faceObj.get("cullface").getAsString().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                }
            }

            float[] uv = null;
            if (faceObj.has("uv")) {
                JsonArray uvArr = faceObj.getAsJsonArray("uv");
                if (uvArr.size() == 4) {
                    uv = new float[]{
                        uvArr.get(0).getAsFloat(), uvArr.get(1).getAsFloat(),
                        uvArr.get(2).getAsFloat(), uvArr.get(3).getAsFloat()
                    };
                }
            }

            int rotation = 0;
            if (faceObj.has("rotation")) {
                rotation = faceObj.get("rotation").getAsInt();
            }

            int tintIndex = -1;
            if (faceObj.has("tintindex")) {
                tintIndex = faceObj.get("tintindex").getAsInt();
            }

            faces.put(orientation, new Face(texture, cullFace, uv, rotation, tintIndex));
        }

        if (faces.isEmpty()) {
            throw new JsonParseException("Element has no faces");
        }
        return faces;
    }
}
