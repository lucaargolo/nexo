package dev.lucaargolo.nexo.api.unit;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.role.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@SuppressWarnings("TypeParameterUnusedInFormals")
public abstract class Unit<C extends Role> {

    @NotNull
    private final Nexo nexo;
    @NotNull
    private final Feature<?, ?> feature;
    @Nullable
    private final C role;

    protected Unit(@NotNull Nexo nexo, @NotNull Feature<?, ?> feature, @Nullable C role) {
        this.nexo = nexo;
        this.feature = feature;
        this.role = role;
    }

    public @NotNull Nexo nexo() {
        return nexo;
    }

    public @NotNull Feature<?, ?> feature() {
        return feature;
    }

    public @Nullable C role() {
        return role;
    }

    public abstract <D> @Nullable D getData(@NotNull DataBase<D> data);

    public abstract <D> void setData(@NotNull DataBase<D> data, @Nullable D d);

    public <D> void setInitialData(@NotNull DataBase<D> data) {
        this.setData(data, data.initial());
    }

    public @NotNull <D, U extends Unit<?>> U withData(@NotNull DataBase<D> data, @NotNull Function<D, D> function) {
        D d = this.getData(data);
        if (d == null) {
            d = data.initial();
        }
        this.setData(data, function.apply(d));
        Class<U> clazz = Nexo.type(this.getClass());
        return clazz.cast(this);
    }

    public @NotNull <R extends Role, U extends Unit<R>> U withRole(@NotNull Class<R> type) {
        this.feature.get(type);
        Class<U> clazz = Nexo.type(this.getClass());
        return clazz.cast(this);
    }

}
