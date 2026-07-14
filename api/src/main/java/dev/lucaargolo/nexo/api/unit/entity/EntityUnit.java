package dev.lucaargolo.nexo.api.unit.entity;

import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;

public abstract class EntityUnit<T extends EntityBase> extends Unit<T> {

    protected EntityUnit(@NotNull T feature) {
        super(feature);
    }

}
