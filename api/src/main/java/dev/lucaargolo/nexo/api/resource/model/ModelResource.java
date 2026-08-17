package dev.lucaargolo.nexo.api.resource.model;

import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.render.model.Mesh;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.render.model.loader.MinecraftModelLoader;
import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class ModelResource extends Resource<ModelResource> {

    private final @NotNull Supplier<Model> supplier;
    protected @Nullable Model model;

    public ModelResource(@NotNull Location location, @NotNull Supplier<Model> supplier) {
        super(location);
        this.supplier = supplier;
    }

    @Override
    public final @NotNull Type<ModelResource> type() {
        return Type.MODEL;
    }

    @Override
    public byte @Nullable [] data() {
        return this.model().data();
    }

    public @NotNull Model model() {
        if (model == null) {
            model = Objects.requireNonNull(supplier.get());
        }
        return model;
    }

    public static @NotNull ModelResource full(@NotNull Location location) {
        return full(location.withoutExtension(), Material.of(location));
    }

    public static @NotNull ModelResource full(@NotNull Location location, @NotNull Material<?> material) {
        float[] vertices = MinecraftModelLoader.boxVertices(0, 0, 0, 16, 16, 16);
        return new ModelResource(location, () -> new Model(new byte[0], List.of(new Mesh(PrimitiveType.QUADS, "all", vertices)), Map.of("all", material), Map.of(
                Location.of("minecraft", "gui"), new Transform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.625f, 0.625f, 0.625f)),
                Location.of("minecraft", "ground"), new Transform(new Vector3f(0, 0, 0), new Vector3f(0, 3, 0), new Vector3f(0.25f, 0.25f, 0.25f)),
                Location.of("minecraft", "fixed"), new Transform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f)),
                Location.of("minecraft", "thirdperson_righthand"), new Transform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f, 0), new Vector3f(0.375f, 0.375f, 0.375f)),
                Location.of("minecraft", "firstperson_righthand"), new Transform(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f)),
                Location.of("minecraft", "firstperson_lefthand"), new Transform(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f))
        ), true)) {
            @Override
            public boolean resolved() {
                return true;
            }
        };
    }


}
