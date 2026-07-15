package dev.lucaargolo.nexo.api.unit.entity;

import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class EntityUnit<C extends Role> extends Unit<C> {

    protected EntityUnit(@NotNull EntityBase feature, @Nullable C role) {
        super(feature, role);
    }

    @Override
    public @NotNull <R extends Role> EntityUnit<R> with(@NotNull Class<R> type) {
        return (EntityUnit<R>) super.with(type);
    }

}
