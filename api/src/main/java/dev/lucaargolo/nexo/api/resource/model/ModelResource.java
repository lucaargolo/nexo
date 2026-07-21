package dev.lucaargolo.nexo.api.resource.model;

import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public class ModelResource extends Resource<ModelResource> {

    private final @NotNull Supplier<Model> supplier;
    private @Nullable Model model;

    public ModelResource(@NotNull Location location, @NotNull Supplier<Model> supplier) {
        super(location);
        this.supplier = supplier;
    }

    @Override
    public final @NotNull Type<ModelResource> type() {
        return Type.MINECRAFT_MODEL;
    }

    public @NotNull Model model() {
        if (model == null) {
            model = Objects.requireNonNull(supplier.get());
        }
        return model;
    }

}
