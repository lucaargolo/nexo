package dev.lucaargolo.nexo.api.feature.entity;

import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class SimpleEntity extends EntityBase {

    private final @Nullable Renderer<Graphics3D, EntityUnit> renderer;

    public SimpleEntity(
            @Nullable Renderer<Graphics3D, EntityUnit> renderer
    ) {
        super();
        this.renderer = renderer;
    }

    public SimpleEntity(
            @NotNull Supplier<Role> role,
            @Nullable Renderer<Graphics3D, EntityUnit> renderer
    ) {
        super(role);
        this.renderer = renderer;
    }

    @Override
    public @Nullable Renderer<Graphics3D, EntityUnit> renderer() {
        return renderer;
    }
}
