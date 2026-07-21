package dev.lucaargolo.nexo.api.resource.model;

import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

public class ModelResource extends Resource<ModelResource> {

    @NotNull
    private final Model model;

    public ModelResource(@NotNull Location location, @NotNull Model model) {
        super(location);
        this.model = model;
    }

    @Override
    public final @NotNull Type<ModelResource> type() {
        return Type.MINECRAFT_MODEL;
    }

    public @NotNull Model model() {
        return model;
    }

}
