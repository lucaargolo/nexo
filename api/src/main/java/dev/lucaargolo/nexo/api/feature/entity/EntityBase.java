package dev.lucaargolo.nexo.api.feature.entity;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.RendererProvider;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class EntityBase extends Feature<EntityBase, EntityUnit<?>> implements RendererProvider<EntityUnit<?>> {

    public EntityBase(@NotNull Location location) {
        super(location);
    }

    public EntityBase(@NotNull Location location, @NotNull Supplier<Role> role) {
        super(location, role);
    }

    @Override
    public final @NotNull Type<EntityBase, EntityUnit<?>> type() {
        return Type.ENTITY;
    }
}
