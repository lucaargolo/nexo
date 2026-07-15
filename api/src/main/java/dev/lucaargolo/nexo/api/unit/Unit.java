package dev.lucaargolo.nexo.api.unit;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

}
