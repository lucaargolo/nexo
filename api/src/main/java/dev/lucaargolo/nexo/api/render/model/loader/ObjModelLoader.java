package dev.lucaargolo.nexo.api.render.model.loader;

import de.javagl.obj.*;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.model.Mesh;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.render.model.ModelMaterial;
import dev.lucaargolo.nexo.api.render.util.BlendMode;
import dev.lucaargolo.nexo.api.render.util.CullMode;
import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ObjModelLoader implements ModelLoader {

    private static final @NotNull String DEFAULT_MATERIAL = "default";

    @Override
    public boolean supports(@NotNull Location path) {
        return path.path().toLowerCase(Locale.ROOT).endsWith(".obj");
    }

    @Override
    public @NotNull Model load(@NotNull Nexo nexo, @NotNull Location path, byte @NotNull [] data) throws Exception {
        Obj source = ObjReader.read(new ByteArrayInputStream(data));
        Obj obj = ObjUtils.triangulate(source);
        List<VertexColor> vertexColors = parseVertexColors(data);
        if (vertexColors.size() != obj.getNumVertices()) {
            throw new IllegalArgumentException("OBJ vertex color count does not match its vertex count");
        }
        Map<String, ModelMaterial> materials = loadMaterials(nexo, path, source.getMtlFileNames());
        materials.putIfAbsent(DEFAULT_MATERIAL, new ModelMaterial(null));

        Map<String, FloatBuilder> geometry = new LinkedHashMap<>();
        String activeMaterial = DEFAULT_MATERIAL;
        for (int faceIndex = 0; faceIndex < obj.getNumFaces(); faceIndex++) {
            ObjFace face = obj.getFace(faceIndex);
            String activated = obj.getActivatedMaterialGroupName(face);
            if (activated != null) activeMaterial = activated;
            materials.putIfAbsent(activeMaterial, new ModelMaterial(null));
            appendFace(obj, face, vertexColors, geometry.computeIfAbsent(activeMaterial, ignored -> new FloatBuilder()));
        }

        List<Mesh> meshes = new ArrayList<>(geometry.size());
        geometry.forEach((material, vertices) -> meshes.add(
                new Mesh(PrimitiveType.TRIANGLES, material, vertices.toArray())
        ));
        if (meshes.isEmpty()) throw new IllegalArgumentException("OBJ contains no faces");
        return new Model(meshes, materials, Map.of(), true);
    }

    private static @NotNull Map<String, ModelMaterial> loadMaterials(
            @NotNull Nexo nexo,
            @NotNull Location objPath,
            @NotNull List<String> libraries
    ) throws Exception {
        Map<String, ModelMaterial> result = new LinkedHashMap<>();
        for (String library : libraries) {
            Location mtlPath = ModelResources.resolve(objPath, library);
            byte[] data = nexo.loadResource(mtlPath);
            if (data == null) throw new IllegalArgumentException("Missing OBJ material library: " + mtlPath);
            for (Mtl material : MtlReader.read(new ByteArrayInputStream(data))) {
                FloatTuple diffuse = material.getKd();
                Float dissolve = material.getD();
                float opacity = dissolve == null ? 1.0F : dissolve;
                float[] color = diffuse == null
                        ? new float[]{1, 1, 1, opacity}
                        : new float[]{diffuse.getX(), diffuse.getY(), diffuse.getZ(), opacity};
                Location texture = material.getMapKd() == null
                        ? null
                        : ModelResources.resolve(mtlPath, material.getMapKd());
                BlendMode blend = opacity < 1.0F ? BlendMode.ALPHA : BlendMode.DISABLED;
                result.put(material.getName(), new ModelMaterial(texture, color, CullMode.BACK, blend));
            }
        }
        return result;
    }

    private static @NotNull List<VertexColor> parseVertexColors(byte @NotNull [] data) {
        String source = new String(data, StandardCharsets.UTF_8)
                .replace("\\\r\n", " ")
                .replace("\\\n", " ");
        List<VertexColor> result = new ArrayList<>();
        source.lines().forEach(line -> {
            int comment = line.indexOf('#');
            String value = comment < 0 ? line : line.substring(0, comment);
            StringTokenizer tokens = new StringTokenizer(value);
            if (!tokens.hasMoreTokens() || !tokens.nextToken().equalsIgnoreCase("v")) return;
            int count = tokens.countTokens();
            float[] components = new float[count];
            for (int i = 0; i < count; i++) components[i] = Float.parseFloat(tokens.nextToken());
            if (count >= 7) {
                result.add(new VertexColor(components[4], components[5], components[6]));
            } else if (count >= 6) {
                result.add(new VertexColor(components[3], components[4], components[5]));
            } else {
                result.add(new VertexColor(1, 1, 1));
            }
        });
        return List.copyOf(result);
    }

    private static void appendFace(
            @NotNull Obj obj,
            @NotNull ObjFace face,
            @NotNull List<VertexColor> vertexColors,
            @NotNull FloatBuilder target
    ) {
        if (face.getNumVertices() != 3) throw new IllegalArgumentException("Triangulated OBJ face is not a triangle");
        Vector3f faceNormal = computeNormal(obj, face);
        for (int i = 0; i < 3; i++) {
            int vertexIndex = face.getVertexIndex(i);
            FloatTuple position = obj.getVertex(vertexIndex);
            FloatTuple texture = face.containsTexCoordIndices() ? obj.getTexCoord(face.getTexCoordIndex(i)) : null;
            FloatTuple normal = face.containsNormalIndices() ? obj.getNormal(face.getNormalIndex(i)) : null;
            VertexColor color = vertexColors.get(vertexIndex);
            target.add(
                    position.getX(), position.getY(), position.getZ(),
                    color.red(), color.green(), color.blue(), 1.0F,
                    texture == null ? 0.0F : texture.getX(),
                    texture == null ? 0.0F : 1.0F - texture.getY(),
                    normal == null ? faceNormal.x : normal.getX(),
                    normal == null ? faceNormal.y : normal.getY(),
                    normal == null ? faceNormal.z : normal.getZ()
            );
        }
    }

    private static @NotNull Vector3f computeNormal(@NotNull Obj obj, @NotNull ObjFace face) {
        FloatTuple a = obj.getVertex(face.getVertexIndex(0));
        FloatTuple b = obj.getVertex(face.getVertexIndex(1));
        FloatTuple c = obj.getVertex(face.getVertexIndex(2));
        Vector3f edgeA = new Vector3f(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ());
        Vector3f edgeB = new Vector3f(c.getX() - a.getX(), c.getY() - a.getY(), c.getZ() - a.getZ());
        Vector3f normal = edgeA.cross(edgeB);
        return normal.lengthSquared() == 0.0F ? normal.set(0, 1, 0) : normal.normalize();
    }

    private record VertexColor(float red, float green, float blue) {
    }

}
