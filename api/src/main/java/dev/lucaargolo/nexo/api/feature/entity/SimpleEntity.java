package dev.lucaargolo.nexo.api.feature.entity;

import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class SimpleEntity extends EntityBase {

    private final @Nullable Renderer<Graphics3D, EntityUnit<?>> renderer;

    public SimpleEntity(@NotNull Location location) {
        super(location);
        this.renderer = null;
    }

    public SimpleEntity(@NotNull Location location, @NotNull Supplier<Role> role) {
        super(location, role);
        this.renderer = null;
    }

    public SimpleEntity(
            @NotNull Location location,
            @NotNull Renderer<Graphics3D, EntityUnit<?>> renderer
    ) {
        super(location);
        this.renderer = renderer;
    }

    public SimpleEntity(
            @NotNull Location location,
            @NotNull Supplier<Role> role,
            @NotNull Renderer<Graphics3D, EntityUnit<?>> renderer
    ) {
        super(location, role);
        this.renderer = renderer;
    }

    @Override
    public @Nullable Renderer<Graphics3D, EntityUnit<?>> renderer() {
        return renderer;
    }
}
