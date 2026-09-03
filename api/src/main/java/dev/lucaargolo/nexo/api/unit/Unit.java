package dev.lucaargolo.nexo.api.unit;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.VaultProvider;
import dev.lucaargolo.nexo.api.role.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Unit<F extends Feature<?, ?>> implements VaultProvider {

    @NotNull
    protected final Nexo nexo;
    @NotNull
    protected final F feature;
    @Nullable
    protected final Role role;

    protected Unit(@NotNull Nexo nexo, @NotNull F feature, @Nullable Role role) {
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

    public @Nullable Role role() {
        return role;
    }

}
