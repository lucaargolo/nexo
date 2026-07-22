package dev.lucaargolo.nexo.api.render.model;

import dev.lucaargolo.nexo.api.render.*;
import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
import dev.lucaargolo.nexo.api.render.util.VertexFormat;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ModelRenderer<U> extends StaticRenderer<Graphics3D, U> {

    private static final float POINT_RADIUS = 1.0F / 128.0F;

    private final @NotNull ModelResource<?> resource;
    private @Nullable CompiledModel compiled;

    public ModelRenderer(@NotNull ModelResource<?> resource) {
        this.resource = resource;
    }

    @Override
    public @NotNull List<@NotNull DrawCall<Graphics3D>> calls(@NotNull U unit) {
        return compiled().calls();
    }

    @Override
    public @NotNull Map<String, Material<?>> materials() {
        return compiled().materials();
    }

    @Override
    public @NotNull Transform transform(@NotNull Location location) {
        Transform transform = compiled().transform(location);
        if (transform != null) return transform;
        return new Transform(new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));
    }

    @Override
    public boolean resolved() {
        return this.resource.resolved();
    }

    @Override
    public boolean shaded() {
        return compiled().shade();
    }

    private @NotNull CompiledModel compiled() {
        if(compiled == null) {
            Model model = this.resource.model();
            List<DrawCall<Graphics3D>> calls = new ArrayList<>(model.meshes().size());
            for (Mesh mesh : model.meshes()) {
                Material<?> material = model.materials().get(mesh.material());
                if (material == null) {
                    throw new IllegalArgumentException("Mesh references unknown material '" + mesh.material() + "'");
                }
                calls.add(graphics -> renderMesh(graphics, mesh, material));
            }
            compiled = new CompiledModel(calls, model.materials(), model.transforms(), model.shade());
        }
        return compiled;
    }

    private static void renderMesh(@NotNull Graphics3D graphics, @NotNull Mesh mesh, @NotNull Material<?> material) {
        graphics.pushState();
        graphics.bindTexture(material.texture().left());
        graphics.color(material.color());
        graphics.cullMode(material.cullMode());
        graphics.blendMode(material.blendMode());
        PrimitiveType primitive = mesh.primitiveType() == PrimitiveType.POINTS ? PrimitiveType.LINES : mesh.primitiveType();
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

    private static void vertex(@NotNull Graphics3D graphics, float @NotNull [] data, int offset, float x, float y, float z) {
        graphics.vertex(
                    data[offset] + x, data[offset + 1] + y, data[offset + 2] + z,
                    data[offset + 3], data[offset + 4], data[offset + 5], data[offset + 6],
                    data[offset + 7], data[offset + 8],
                    data[offset + 9], data[offset + 10], data[offset + 11]
        );
    }

    private record CompiledModel(
            @NotNull List<DrawCall<Graphics3D>> calls,
            @NotNull Map<String, Material<?>> materials,
            @NotNull Map<Location, Transform> transforms,
            boolean shade
    ) {

        public @Nullable Transform transform(Location string) {
            return transforms.get(string);
        }
    }
}
