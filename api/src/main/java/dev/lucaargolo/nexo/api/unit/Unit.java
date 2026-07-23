package dev.lucaargolo.nexo.api.unit;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.role.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("TypeParameterUnusedInFormals")
public abstract class Unit<F extends Feature<?, ?>, C extends Role> {

    @NotNull
    protected final Nexo nexo;
    @NotNull
    protected final F feature;
    @Nullable
    protected final C role;

    protected Unit(@NotNull Nexo nexo, @NotNull F feature, @Nullable C role) {
        this.nexo = nexo;
        this.feature = feature;
        this.role = role;
    }

    public @NotNull Nexo nexo() {
        return nexo;
    }

    public @NotNull F feature() {
        return feature;
    }

    public @Nullable C role() {
        return role;
    }

    public @NotNull <R extends Role, U extends Unit<?, R>> U withRole(@NotNull Class<R> type) {
        this.feature.get(type);
        Class<U> clazz = Nexo.type(this.getClass());
        return clazz.cast(this);
    }

}
