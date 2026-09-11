package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public interface VaultFactory<V extends Unit<?> & VaultProvider> {

    default @NotNull <U extends Unit<?>> Map<String, Function<V, Vault.Slotted<U>>> vaults(@NotNull Class<U> type) {
        return Map.of();
    }

}