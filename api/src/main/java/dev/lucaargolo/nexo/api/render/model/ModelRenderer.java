package dev.lucaargolo.nexo.api.render.model;

import dev.lucaargolo.nexo.api.render.DrawCall;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
import dev.lucaargolo.nexo.api.render.util.VertexFormat;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.*;

public final class ModelRenderer<U> extends StaticRenderer<Graphics3D, U> {

    public static final @NotNull Location MISSING_TEXTURE = Location.of("nexo", "null");
    private static final float POINT_RADIUS = 1.0F / 128.0F;

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
        List<DrawCall<Graphics3D>> calls = new ArrayList<>(model.meshes().size());
        Map<String, Location> textures = new LinkedHashMap<>();
        model.materials().forEach((name, material) -> textures.put(
                name,
                material.texture() == null ? Model.WHITE_TEXTURE : material.texture()
        ));
        for (Mesh mesh : model.meshes()) {
            ModelMaterial material = model.materials().get(mesh.material());
            if (material == null) {
                throw new IllegalArgumentException("Mesh references unknown material '" + mesh.material() + "'");
            }
            Location texture = material.texture();
            if (texture == null) texture = Model.WHITE_TEXTURE;
            Location finalTexture = texture;
            calls.add(graphics -> renderMesh(graphics, mesh, material, finalTexture));
        }
        return new CompiledModel(List.copyOf(calls), Collections.unmodifiableMap(textures));
    }

    private static void renderMesh(
            @NotNull Graphics3D graphics,
            @NotNull Mesh mesh,
            @NotNull ModelMaterial material,
            @NotNull Location texture
    ) {
        graphics.pushState();
        graphics.bindTexture(texture);
        graphics.color(material.colorData());
        graphics.cullMode(material.cullMode());
        graphics.blendMode(material.blendMode());
        PrimitiveType primitive = mesh.primitiveType() == PrimitiveType.POINTS
                ? PrimitiveType.LINES : mesh.primitiveType();
        graphics.begin(primitive, VertexFormat.POSITION_COLOR_TEX_NORMAL);
        float[] data = mesh.vertexData();
        for (int offset = 0; offset < data.length; offset += Mesh.VERTEX_STRIDE) {
            if (mesh.primitiveType() == PrimitiveType.POINTS) {
                vertex(graphics, data, offset, -POINT_RADIUS, 0, 0);
                vertex(graphics, data, offset, POINT_RADIUS, 0, 0);
                vertex(graphics, data, offset, 0, -POINT_RADIUS, 0);
                vertex(graphics, data, offset, 0, POINT_RADIUS, 0);
                vertex(graphics, data, offset, 0, 0, -POINT_RADIUS);
                vertex(graphics, data, offset, 0, 0, POINT_RADIUS);
            } else {
                vertex(graphics, data, offset, 0, 0, 0);
            }
        }
        graphics.end();
        graphics.popState();
    }

    private static void vertex(
            @NotNull Graphics3D graphics,
            float @NotNull [] data,
            int offset,
            float x,
            float y,
            float z
    ) {
        graphics.vertex(
                    data[offset] + x, data[offset + 1] + y, data[offset + 2] + z,
                    data[offset + 3], data[offset + 4], data[offset + 5], data[offset + 6],
                    data[offset + 7], data[offset + 8],
                    data[offset + 9], data[offset + 10], data[offset + 11]
        );
    }

    private record CompiledModel(
            @NotNull List<DrawCall<Graphics3D>> calls,
            @NotNull Map<String, Location> textures
    ) {
    }
}
