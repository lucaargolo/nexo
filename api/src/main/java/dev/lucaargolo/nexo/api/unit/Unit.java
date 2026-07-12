package dev.lucaargolo.nexo.api.unit;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Unit<T extends Feature<T>> {

    @NotNull
    private final T feature;

    protected Unit(@NotNull Class<T> type, @NotNull T feature) {
        this.feature = feature;
    }

    public @Nullable T value() {
        return feature;
    }

    public abstract <D> @Nullable D getData(@NotNull DataBase<D> data);

    public abstract <D> void setData(@NotNull DataBase<D> data, @Nullable D d);

}
