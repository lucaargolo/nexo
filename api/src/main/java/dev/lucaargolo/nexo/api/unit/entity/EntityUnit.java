package dev.lucaargolo.nexo.api.unit.entity;

import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class EntityUnit<C extends Role> extends Unit<C> {

    protected EntityUnit(@NotNull EntityBase feature, @Nullable C role) {
        super(feature, role);
    }

}
