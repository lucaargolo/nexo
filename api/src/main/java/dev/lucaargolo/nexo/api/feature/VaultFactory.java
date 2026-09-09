package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public interface VaultFactory<V extends Unit<?> & VaultProvider> {

    //TODO: Theoretically we should implement this in MinecraftFeature's as a simple maper unit -> unit.vault just for parity
    default @NotNull <U extends Unit<?>> Map<String, Function<V, Vault<U>>> vaults(@NotNull Class<U> type) {
        return Map.of();
    };

}
