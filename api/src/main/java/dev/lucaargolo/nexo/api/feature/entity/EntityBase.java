package dev.lucaargolo.nexo.api.feature.entity;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

public abstract class EntityBase extends Feature<EntityBase> {

    public EntityBase(@NotNull Location location) {
        super(location);
    }

    @Override
    public final @NotNull Type<EntityBase> type() {
        return Type.ENTITY;
    }
}
