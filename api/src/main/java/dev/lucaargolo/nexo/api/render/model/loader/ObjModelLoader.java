package dev.lucaargolo.nexo.api.render.model.loader;

import de.javagl.obj.*;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.model.Mesh;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.render.model.ModelResources;
import dev.lucaargolo.nexo.api.render.util.*;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ObjModelLoader implements ModelLoader {

    @Override
    public boolean supports(@NotNull Location path) {
        return path.path().toLowerCase(Locale.ROOT).endsWith(".obj");
    }

    @Override
    public @NotNull List<@NotNull String> extensions() {
        return List.of(".obj");
    }

    @Override
    public @NotNull Model load(@NotNull Nexo nexo, @NotNull Location path, byte @NotNull [] data) throws Exception {
        Obj source = ObjReader.read(new ByteArrayInputStream(data));
        Obj obj = ObjUtils.triangulate(source);
        String colorSource = new String(data, StandardCharsets.UTF_8)
                .replace("\\\r\n", " ")
                .replace("\\\n", " ");
        List<float[]> parsedVertexColors = new ArrayList<>();
        for (String line : colorSource.lines().toList()) {
            int comment = line.indexOf('#');
            String value = comment < 0 ? line : line.substring(0, comment);
            StringTokenizer tokens = new StringTokenizer(value);
            if (!tokens.hasMoreTokens() || !tokens.nextToken().equalsIgnoreCase("v")) {
                continue;
            }
            int count = tokens.countTokens();
            float[] components = new float[count];
            for (int i = 0; i < count; i++) {
                components[i] = Float.parseFloat(tokens.nextToken());
            }
            if (count >= 7) {
                parsedVertexColors.add(new float[]{components[4], components[5], components[6]});
            } else if (count == 6) {
                parsedVertexColors.add(new float[]{components[3], components[4], components[5]});
            } else {
                parsedVertexColors.add(new float[]{1, 1, 1});
            }
        }
        List<float[]> vertexColors = List.copyOf(parsedVertexColors);
        if (vertexColors.size() != obj.getNumVertices()) {
            throw new IllegalArgumentException("OBJ vertex color count does not match its vertex count");
        }

        Map<String, Material<?>> materials = new LinkedHashMap<>();
        for (String library : source.getMtlFileNames()) {
            Location mtlPath = ModelResources.resolve(path, library);
            byte[] materialData = nexo.loadResource(mtlPath);
            if (materialData == null) {
                throw new IllegalArgumentException("Missing OBJ material library: " + mtlPath);
            }
            for (Mtl material : MtlReader.read(new ByteArrayInputStream(materialData))) {
                FloatTuple diffuse = material.getKd();
                Float dissolve = material.getD();
                float opacity = dissolve == null ? 1.0F : dissolve;
                float[] color = diffuse == null
                        ? new float[]{1, 1, 1, opacity}
                        : new float[]{diffuse.getX(), diffuse.getY(), diffuse.getZ(), opacity};
                String mapKd = material.getMapKd();
                Location texture = mapKd == null ? null : ModelResources.resolve(mtlPath, mapKd);
                BlendMode blend = opacity < 1.0F ? BlendMode.ALPHA : BlendMode.DISABLED;
                LayerMode layer = opacity < 1.0F ? LayerMode.TRANSLUCENT : LayerMode.SOLID;
                if (texture != null) {
                    materials.put(material.getName(), new Material<>(texture, texture, color, CullMode.BACK, blend, layer));
                } else {
                    materials.put(material.getName(), new Material<Location>(null, color, CullMode.BACK, blend, layer));
                }
            }
        }
        materials.putIfAbsent("default", Material.untextured());
        Map<String, FloatBuilder> geometry = new LinkedHashMap<>();

        String activeMaterial = "default";
        for (int faceIndex = 0; faceIndex < obj.getNumFaces(); faceIndex++) {
            ObjFace face = obj.getFace(faceIndex);
            activeMaterial = Objects.requireNonNullElse(obj.getActivatedMaterialGroupName(face),  activeMaterial);
            appendFace(obj, face, vertexColors, geometry.computeIfAbsent(activeMaterial, ignored -> new FloatBuilder()));
        }

        List<Mesh> meshes = new ArrayList<>(geometry.size());
        geometry.forEach((material, vertices) -> meshes.add(
                new Mesh(PrimitiveType.TRIANGLES, material, vertices.toArray())
        ));
        if (meshes.isEmpty()) {
            throw new IllegalArgumentException("OBJ contains no faces");
        }
        return new Model(data, meshes, materials, Map.of(), true);
    }

    private static void appendFace(@NotNull Obj obj, @NotNull ObjFace face, @NotNull List<float[]> vertexColors, @NotNull FloatBuilder target) {
        if (face.getNumVertices() != 3) {
            throw new IllegalArgumentException("Triangulated OBJ face is not a triangle");
        }
        FloatTuple a = obj.getVertex(face.getVertexIndex(0));
        FloatTuple b = obj.getVertex(face.getVertexIndex(1));
        FloatTuple c = obj.getVertex(face.getVertexIndex(2));
        Vector3f edgeA = new Vector3f(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ());
        Vector3f edgeB = new Vector3f(c.getX() - a.getX(), c.getY() - a.getY(), c.getZ() - a.getZ());
        Vector3f faceNormal = edgeA.cross(edgeB);
        if (faceNormal.lengthSquared() == 0.0F) {
            faceNormal.set(0, 1, 0);
        } else {
            faceNormal.normalize();
        }
        for (int i = 0; i < 3; i++) {
            int vertexIndex = face.getVertexIndex(i);
            FloatTuple position = obj.getVertex(vertexIndex);
            FloatTuple texture = face.containsTexCoordIndices() ? obj.getTexCoord(face.getTexCoordIndex(i)) : null;
            FloatTuple normal = face.containsNormalIndices() ? obj.getNormal(face.getNormalIndex(i)) : null;
            float[] color = vertexColors.get(vertexIndex);
            target.add(
                    position.getX(), position.getY(), position.getZ(),
                    color[0], color[1], color[2], 1.0F,
                    texture == null ? 0.0F : texture.getX(),
                    texture == null ? 0.0F : 1.0F - texture.getY(),
                    normal == null ? faceNormal.x : normal.getX(),
                    normal == null ? faceNormal.y : normal.getY(),
                    normal == null ? faceNormal.z : normal.getZ()
            );
        }
    }

}
