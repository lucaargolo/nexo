package dev.lucaargolo.nexo.api.unit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface VaultProvider {

    @NotNull <U extends Unit<?, ?>> Set<String> vaults(@NotNull Class<U> type);

    @Nullable <U extends Unit<?, ?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key);

}
