package dev.lucaargolo.nexo.api.unit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface VaultProvider {

    @Nullable <U extends Unit<?, ?>> Vault<U> container(@NotNull Class<U> type, @NotNull String key);

}
