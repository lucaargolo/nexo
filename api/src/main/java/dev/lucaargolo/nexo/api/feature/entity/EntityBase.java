package dev.lucaargolo.nexo.api.feature.entity;

import dev.lucaargolo.nexo.api.feature.*;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class EntityBase extends Feature<EntityBase, EntityUnit> implements VaultFactory<EntityUnit>, RendererProvider<Graphics3D, EntityUnit>, TickerProvider<EntityUnit>, DataInitializer {

    public EntityBase() {

    }

    public EntityBase(@NotNull Supplier<Role> role) {
        super(role);
    }

    @Override
    public final @NotNull Type<EntityBase, EntityUnit> type() {
        return Type.ENTITY;
    }
}
