package dev.lucaargolo.nexo.api.render.model.loader;

import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.io.GltfAsset;
import de.javagl.jgltf.model.io.GltfAssetReader;
import de.javagl.jgltf.model.io.GltfReference;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.model.Mesh;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.render.model.ModelMaterial;
import dev.lucaargolo.nexo.api.render.util.BlendMode;
import dev.lucaargolo.nexo.api.render.util.CullMode;
import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public final class GltfModelLoader implements ModelLoader {

    private static final @NotNull String DEFAULT_MATERIAL = "default";

    @Override
    public boolean supports(@NotNull Location path) {
        String value = path.path().toLowerCase(Locale.ROOT);
        return value.endsWith(".gltf") || value.endsWith(".glb");
    }

    @Override
    public @NotNull Model load(@NotNull Nexo nexo, @NotNull Location path, byte @NotNull [] data) throws Exception {
        GltfAssetReader reader = new GltfAssetReader();
        GltfAsset asset = reader.readWithoutReferences(new ByteArrayInputStream(data));
        if (!(asset instanceof GltfAssetV2)) {
            throw new IllegalArgumentException("Only glTF 2.0 assets are supported");
        }
        resolveReferences(nexo, path, asset);
        GltfModel gltf = GltfModels.create(asset);

        IdentityHashMap<MaterialModel, String> materialKeys = new IdentityHashMap<>();
        IdentityHashMap<ImageModel, Location> imageLocations = new IdentityHashMap<>();
        Map<Location, byte[]> embeddedTextures = new LinkedHashMap<>();
        Map<String, ModelMaterial> materials = createMaterials(
                path, gltf, materialKeys, imageLocations, embeddedTextures
        );
        boolean usesDefaultMaterial = gltf.getMeshModels().stream()
                .flatMap(mesh -> mesh.getMeshPrimitiveModels().stream())
                .anyMatch(primitive -> primitive.getMaterialModel() == null);
        if (usesDefaultMaterial) materials.put(DEFAULT_MATERIAL, new ModelMaterial(null));

        List<Mesh> meshes = new ArrayList<>();
        for (NodeModel node : sceneNodes(asset, gltf)) {
            appendNodeMeshes(node, materialKeys, meshes);
        }
        if (meshes.isEmpty()) throw new IllegalArgumentException("glTF contains no renderable mesh primitives");
        return new Model(meshes, materials, Map.of(), true, embeddedTextures);
    }

    private static void resolveReferences(
            @NotNull Nexo nexo,
            @NotNull Location modelPath,
            @NotNull GltfAsset asset
    ) {
        for (GltfReference reference : asset.getReferences()) {
            Location resource = ModelResources.resolve(modelPath, reference.getUri());
            byte[] referencedData = nexo.loadResource(resource);
            if (referencedData == null) {
                throw new IllegalArgumentException("Missing glTF " + reference.getName() + ": " + resource);
            }
            reference.getTarget().accept(ByteBuffer.wrap(referencedData).order(ByteOrder.LITTLE_ENDIAN));
        }
    }

    private static @NotNull List<NodeModel> sceneNodes(@NotNull GltfAsset asset, @NotNull GltfModel model) {
        List<SceneModel> scenes = model.getSceneModels();
        if (!scenes.isEmpty()) {
            int scene = 0;
            if (asset instanceof GltfAssetV2 assetV2 && assetV2.getGltf().getScene() != null) {
                scene = assetV2.getGltf().getScene();
            }
            if (scene < 0 || scene >= scenes.size()) throw new IllegalArgumentException("Invalid default glTF scene: " + scene);
            return flatten(scenes.get(scene).getNodeModels());
        }
        List<NodeModel> roots = model.getNodeModels().stream().filter(node -> node.getParent() == null).toList();
        return flatten(roots);
    }

    private static @NotNull List<NodeModel> flatten(@NotNull List<NodeModel> roots) {
        List<NodeModel> result = new ArrayList<>();
        for (NodeModel root : roots) appendNode(root, result);
        return result;
    }

    private static void appendNode(@NotNull NodeModel node, @NotNull List<NodeModel> result) {
        result.add(node);
        for (NodeModel child : node.getChildren()) appendNode(child, result);
    }

    private static @NotNull Map<String, ModelMaterial> createMaterials(
            @NotNull Location modelPath,
            @NotNull GltfModel gltf,
            @NotNull Map<MaterialModel, String> materialKeys,
            @NotNull Map<ImageModel, Location> imageLocations,
            @NotNull Map<Location, byte[]> embeddedTextures
    ) {
        Map<String, ModelMaterial> result = new LinkedHashMap<>();
        List<MaterialModel> source = gltf.getMaterialModels();
        for (int i = 0; i < source.size(); i++) {
            MaterialModel material = source.get(i);
            String name = material.getName() == null || material.getName().isBlank()
                    ? "material_" + i
                    : uniqueName(result, material.getName());
            materialKeys.put(material, name);
            if (material instanceof MaterialModelV2 pbr) {
                Location texture = textureLocation(
                        modelPath, pbr.getBaseColorTexture(), gltf, imageLocations, embeddedTextures
                );
                float[] factor = pbr.getBaseColorFactor().clone();
                CullMode cull = pbr.isDoubleSided() ? CullMode.DISABLED : CullMode.BACK;
                BlendMode blend = pbr.getAlphaMode() == MaterialModelV2.AlphaMode.BLEND
                        ? BlendMode.ALPHA : BlendMode.DISABLED;
                result.put(name, new ModelMaterial(texture, factor, cull, blend));
            } else {
                result.put(name, new ModelMaterial(null));
            }
        }
        return result;
    }

    private static @NotNull String uniqueName(@NotNull Map<String, ?> values, @NotNull String requested) {
        if (!values.containsKey(requested)) return requested;
        int suffix = 2;
        while (values.containsKey(requested + "_" + suffix)) suffix++;
        return requested + "_" + suffix;
    }

    private static @Nullable Location textureLocation(
            @NotNull Location modelPath,
            @Nullable TextureModel texture,
            @NotNull GltfModel gltf,
            @NotNull Map<ImageModel, Location> locations,
            @NotNull Map<Location, byte[]> embeddedTextures
    ) {
        if (texture == null) return null;
        ImageModel image = texture.getImageModel();
        Location existing = locations.get(image);
        if (existing != null) return existing;
        ByteBuffer imageData = image.getImageData();
        if (imageData == null) throw new IllegalArgumentException("glTF texture has no image data");
        byte[] bytes = new byte[imageData.remaining()];
        imageData.get(bytes);
        int imageIndex = gltf.getImageModels().indexOf(image);
        String mimeType = image.getMimeType() == null ? "image/png" : image.getMimeType();
        String uri = image.getUri() == null ? "" : image.getUri();
        Location location = modelPath.withoutExtension().withPathSuffix(
                ".textures/image_" + imageIndex + ModelResources.extension(mimeType, uri)
        );
        locations.put(image, location);
        embeddedTextures.put(location, bytes);
        return location;
    }

    private static void appendNodeMeshes(
            @NotNull NodeModel node,
            @NotNull Map<MaterialModel, String> materialKeys,
            @NotNull List<Mesh> meshes
    ) {
        Matrix4f transform = new Matrix4f().set(node.computeGlobalTransform(null));
        Matrix3f normalTransform = new Matrix3f(transform).invert().transpose();
        for (MeshModel mesh : node.getMeshModels()) {
            float[] weights = node.getWeights() != null ? node.getWeights() : mesh.getWeights();
            for (MeshPrimitiveModel primitive : mesh.getMeshPrimitiveModels()) {
                meshes.add(createMesh(primitive, weights, transform, normalTransform, materialKeys));
            }
        }
    }

    private static @NotNull Mesh createMesh(
            @NotNull MeshPrimitiveModel primitive,
            float @Nullable [] weights,
            @NotNull Matrix4f transform,
            @NotNull Matrix3f normalTransform,
            @NotNull Map<MaterialModel, String> materialKeys
    ) {
        AccessorModel positions = primitive.getAttributes().get("POSITION");
        if (positions == null) throw new IllegalArgumentException("glTF mesh primitive has no POSITION attribute");
        int[] indices = indices(primitive, positions.getCount());
        Topology topology = topology(primitive.getMode(), indices);
        int[] expanded = topology.indices();

        AccessorModel texCoords = textureCoordinates(primitive);
        AccessorModel colors = primitive.getAttributes().get("COLOR_0");
        AccessorModel normals = primitive.getAttributes().get("NORMAL");
        validateAttribute("POSITION", positions, positions.getCount(), 3, 3);
        validateAttribute("NORMAL", normals, positions.getCount(), 3, 3);
        validateAttribute("TEXCOORD", texCoords, positions.getCount(), 2, 2);
        validateAttribute("COLOR_0", colors, positions.getCount(), 3, 4);
        FloatAccessor positionData = new FloatAccessor(positions);
        FloatAccessor textureData = texCoords == null ? null : new FloatAccessor(texCoords);
        FloatAccessor colorData = colors == null ? null : new FloatAccessor(colors);
        FloatAccessor normalData = normals == null ? null : new FloatAccessor(normals);
        List<MorphTarget> morphTargets = createMorphTargets(weights, primitive.getTargets(), positions.getCount());

        float[] vertices = new float[expanded.length * Mesh.VERTEX_STRIDE];
        for (int output = 0; output < expanded.length; output++) {
            int source = expanded[output];
            int offset = output * Mesh.VERTEX_STRIDE;
            Vector3f position = positionData.vector3(source);
            Vector3f normal = normalData == null
                    ? topology.type() == PrimitiveType.TRIANGLES ? new Vector3f() : new Vector3f(0, 1, 0)
                    : normalData.vector3(source);
            applyMorphTargets(position, normal, source, morphTargets);
            transform.transformPosition(position);
            if (normalData != null) normalTransform.transform(normal).normalize();

            vertices[offset] = position.x;
            vertices[offset + 1] = position.y;
            vertices[offset + 2] = position.z;
            vertices[offset + 3] = colorData == null ? 1.0F : colorData.get(source, 0);
            vertices[offset + 4] = colorData == null ? 1.0F : colorData.get(source, 1);
            vertices[offset + 5] = colorData == null ? 1.0F : colorData.get(source, 2);
            vertices[offset + 6] = colorData == null || colorData.components() < 4 ? 1.0F : colorData.get(source, 3);
            vertices[offset + 7] = textureData == null ? 0.0F : textureData.get(source, 0);
            vertices[offset + 8] = textureData == null ? 0.0F : textureData.get(source, 1);
            vertices[offset + 9] = normal.x;
            vertices[offset + 10] = normal.y;
            vertices[offset + 11] = normal.z;
        }
        if (normalData == null && topology.type() == PrimitiveType.TRIANGLES) generateNormals(vertices);

        MaterialModel material = primitive.getMaterialModel();
        String materialKey = material == null ? DEFAULT_MATERIAL : materialKeys.get(material);
        if (materialKey == null) throw new IllegalArgumentException("glTF primitive references an unknown material");
        return new Mesh(topology.type(), materialKey, vertices);
    }

    private static @Nullable AccessorModel textureCoordinates(@NotNull MeshPrimitiveModel primitive) {
        int set = 0;
        if (primitive.getMaterialModel() instanceof MaterialModelV2 pbr && pbr.getBaseColorTexcoord() != null) {
            set = pbr.getBaseColorTexcoord();
        }
        return primitive.getAttributes().get("TEXCOORD_" + set);
    }

    private static void validateAttribute(
            @NotNull String name,
            @Nullable AccessorModel accessor,
            int expectedCount,
            int minimumComponents,
            int maximumComponents
    ) {
        if (accessor == null) return;
        int components = accessor.getElementType().getNumComponents();
        if (components < minimumComponents || components > maximumComponents) {
            throw new IllegalArgumentException(
                    "glTF " + name + " attribute must have "
                            + (minimumComponents == maximumComponents
                            ? minimumComponents
                            : minimumComponents + " or " + maximumComponents)
                            + " components, found " + components
            );
        }
        if (accessor.getCount() != expectedCount) {
            throw new IllegalArgumentException(
                    "glTF " + name + " attribute has " + accessor.getCount()
                            + " elements, expected " + expectedCount
            );
        }
    }

    private static @NotNull List<MorphTarget> createMorphTargets(
            float @Nullable [] weights,
            @NotNull List<Map<String, AccessorModel>> targets,
            int vertexCount
    ) {
        if (weights == null) return List.of();
        List<MorphTarget> result = new ArrayList<>();
        int count = Math.min(weights.length, targets.size());
        for (int i = 0; i < count; i++) {
            float weight = weights[i];
            if (weight == 0.0F) continue;
            AccessorModel positions = targets.get(i).get("POSITION");
            AccessorModel normals = targets.get(i).get("NORMAL");
            validateAttribute("morph POSITION", positions, vertexCount, 3, 3);
            validateAttribute("morph NORMAL", normals, vertexCount, 3, 3);
            result.add(new MorphTarget(
                    weight,
                    positions == null ? null : new FloatAccessor(positions),
                    normals == null ? null : new FloatAccessor(normals)
            ));
        }
        return List.copyOf(result);
    }

    private static void applyMorphTargets(
            @NotNull Vector3f position,
            @NotNull Vector3f normal,
            int vertex,
            @NotNull List<MorphTarget> targets
    ) {
        for (MorphTarget target : targets) {
            if (target.positions() != null) position.fma(target.weight(), target.positions().vector3(vertex));
            if (target.normals() != null) normal.fma(target.weight(), target.normals().vector3(vertex));
        }
    }

    private static int @NotNull [] indices(@NotNull MeshPrimitiveModel primitive, int vertexCount) {
        AccessorModel accessor = primitive.getIndices();
        if (accessor == null) {
            int[] result = new int[vertexCount];
            for (int i = 0; i < result.length; i++) result[i] = i;
            return result;
        }
        AccessorData data = accessor.getAccessorData();
        ByteBuffer buffer = data.createByteBuffer().order(ByteOrder.LITTLE_ENDIAN);
        int[] result = new int[accessor.getCount()];
        for (int i = 0; i < result.length; i++) {
            result[i] = switch (accessor.getComponentType()) {
                case 5121 -> Byte.toUnsignedInt(buffer.get(i));
                case 5123 -> Short.toUnsignedInt(buffer.getShort(i * Short.BYTES));
                case 5125 -> buffer.getInt(i * Integer.BYTES);
                default -> throw new IllegalArgumentException("Unsupported glTF index component type: " + accessor.getComponentType());
            };
        }
        return result;
    }

    private static @NotNull Topology topology(int mode, int @NotNull [] indices) {
        return switch (mode) {
            case 0 -> new Topology(PrimitiveType.POINTS, indices);
            case 1 -> new Topology(PrimitiveType.LINES, indices);
            case 2 -> new Topology(PrimitiveType.LINE_LOOP, indices);
            case 3 -> new Topology(PrimitiveType.LINE_STRIP, indices);
            case 4 -> new Topology(PrimitiveType.TRIANGLES, indices);
            case 5 -> new Topology(PrimitiveType.TRIANGLES, triangleStrip(indices));
            case 6 -> new Topology(PrimitiveType.TRIANGLES, triangleFan(indices));
            default -> throw new IllegalArgumentException("Unsupported glTF primitive mode: " + mode);
        };
    }

    private static int @NotNull [] triangleStrip(int @NotNull [] indices) {
        if (indices.length < 3) return new int[0];
        int[] result = new int[(indices.length - 2) * 3];
        for (int i = 2; i < indices.length; i++) {
            int offset = (i - 2) * 3;
            result[offset] = indices[(i & 1) == 0 ? i - 2 : i - 1];
            result[offset + 1] = indices[(i & 1) == 0 ? i - 1 : i - 2];
            result[offset + 2] = indices[i];
        }
        return result;
    }

    private static int @NotNull [] triangleFan(int @NotNull [] indices) {
        if (indices.length < 3) return new int[0];
        int[] result = new int[(indices.length - 2) * 3];
        for (int i = 2; i < indices.length; i++) {
            int offset = (i - 2) * 3;
            result[offset] = indices[0];
            result[offset + 1] = indices[i - 1];
            result[offset + 2] = indices[i];
        }
        return result;
    }

    private static void generateNormals(float @NotNull [] vertices) {
        for (int offset = 0; offset < vertices.length; offset += Mesh.VERTEX_STRIDE * 3) {
            Vector3f a = new Vector3f(vertices[offset], vertices[offset + 1], vertices[offset + 2]);
            int bOffset = offset + Mesh.VERTEX_STRIDE;
            int cOffset = bOffset + Mesh.VERTEX_STRIDE;
            Vector3f edgeA = new Vector3f(vertices[bOffset], vertices[bOffset + 1], vertices[bOffset + 2]).sub(a);
            Vector3f edgeB = new Vector3f(vertices[cOffset], vertices[cOffset + 1], vertices[cOffset + 2]).sub(a);
            Vector3f normal = edgeA.cross(edgeB);
            if (normal.lengthSquared() == 0.0F) normal.set(0, 1, 0); else normal.normalize();
            for (int vertex = 0; vertex < 3; vertex++) {
                int normalOffset = offset + vertex * Mesh.VERTEX_STRIDE + 9;
                vertices[normalOffset] = normal.x;
                vertices[normalOffset + 1] = normal.y;
                vertices[normalOffset + 2] = normal.z;
            }
        }
    }

    private record MorphTarget(
            float weight,
            @Nullable FloatAccessor positions,
            @Nullable FloatAccessor normals
    ) {
    }

    private static final class Topology {
        private final @NotNull PrimitiveType type;
        private final int @NotNull [] indices;

        Topology(@NotNull PrimitiveType type, int @NotNull [] indices) {
            this.type = type;
            this.indices = indices;
        }

        @NotNull PrimitiveType type() {
            return type;
        }

        int @NotNull [] indices() {
            return indices;
        }
    }

    private static final class FloatAccessor {
        private final @NotNull AccessorModel accessor;
        private final @NotNull ByteBuffer data;
        private final int components;
        private final int componentSize;

        FloatAccessor(@NotNull AccessorModel accessor) {
            this.accessor = accessor;
            // AccessorData explicitly removes buffer-view offsets and byte strides.
            this.data = accessor.getAccessorData().createByteBuffer().order(ByteOrder.LITTLE_ENDIAN);
            this.components = accessor.getElementType().getNumComponents();
            this.componentSize = accessor.getComponentSizeInBytes();
        }

        int components() {
            return components;
        }

        float get(int element, int component) {
            if (element < 0 || element >= accessor.getCount()) {
                throw new IndexOutOfBoundsException(
                        "glTF accessor element " + element + " is outside [0, " + accessor.getCount() + ")"
                );
            }
            if (component < 0 || component >= components) {
                throw new IndexOutOfBoundsException(
                        "glTF accessor component " + component + " is outside [0, " + components + ")"
                );
            }
            int offset = (element * components + component) * componentSize;
            int type = accessor.getComponentType();
            boolean normalized = accessor.isNormalized();
            return switch (type) {
                case 5120 -> normalized ? Math.max(data.get(offset) / 127.0F, -1.0F) : data.get(offset);
                case 5121 -> normalized ? Byte.toUnsignedInt(data.get(offset)) / 255.0F : Byte.toUnsignedInt(data.get(offset));
                case 5122 -> normalized ? Math.max(data.getShort(offset) / 32767.0F, -1.0F) : data.getShort(offset);
                case 5123 -> normalized ? Short.toUnsignedInt(data.getShort(offset)) / 65535.0F : Short.toUnsignedInt(data.getShort(offset));
                case 5125 -> normalized
                        ? (float) (Integer.toUnsignedLong(data.getInt(offset)) / 4294967295.0)
                        : Integer.toUnsignedLong(data.getInt(offset));
                case 5126 -> data.getFloat(offset);
                default -> throw new IllegalArgumentException("Unsupported glTF accessor component type: " + type);
            };
        }

        @NotNull Vector3f vector3(int element) {
            return new Vector3f(get(element, 0), get(element, 1), get(element, 2));
        }
    }
}
