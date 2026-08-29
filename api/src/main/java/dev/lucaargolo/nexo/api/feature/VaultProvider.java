package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface VaultProvider {

    default @NotNull <U extends Unit<?, ?>> Set<String> vaults(@NotNull Class<U> type) {
        return Set.of();
    }

    default @Nullable <U extends Unit<?, ?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        return null;
    }

}
