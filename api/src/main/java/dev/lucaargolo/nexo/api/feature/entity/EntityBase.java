package dev.lucaargolo.nexo.api.feature.entity;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class EntityBase extends Feature<EntityBase> {

    public EntityBase(@NotNull Location location) {
        super(location);
    }

    public EntityBase(@NotNull Location location, @Nullable Role role) {
        super(location, role);
    }

    @Override
    public final @NotNull Type<EntityBase> type() {
        return Type.ENTITY;
    }
}
