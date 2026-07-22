package dev.lucaargolo.nexo.api.unit;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.role.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public abstract class Unit<C extends Role> {

    @NotNull
    private final Feature<?> feature;
    @Nullable
    private final C role;

    protected Unit(@NotNull Feature<?> feature, @Nullable C role) {
        this.feature = feature;
        this.role = role;
    }

    public @NotNull Feature<?> feature() {
        return feature;
    }

    public @Nullable C role() {
        return role;
    }

    public abstract <D> @Nullable D getData(@NotNull DataBase<D> data);

    public abstract <D> void setData(@NotNull DataBase<D> data, @Nullable D d);

    @SuppressWarnings("unchecked")
    public @NotNull <D, U extends Unit<?>> U withData(@NotNull DataBase<D> data, @NotNull Function<D, D> function) {
        D d = this.getData(data);
        if (d == null) {
            d = data.initial();
        }
        this.setData(data, function.apply(d));
        return (U) this;
    }

    @SuppressWarnings("unchecked")
    public @NotNull <R extends Role, U extends Unit<R>> U withRole(@NotNull Class<R> type) {
        this.feature.get(type);
        return (U) this;
    }

}
