package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public interface VaultFactoryProvider<U extends Unit<?, ?>> {

    default <V extends Unit<?, ?>> @NotNull Map<String, Function<U, ? extends @Nullable Vault<V>>> vaults(@NotNull Class<V> type) {
        return Map.of();
    }

}
