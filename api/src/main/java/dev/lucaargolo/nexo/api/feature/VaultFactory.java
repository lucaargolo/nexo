package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Function;

public interface VaultFactory<V extends Unit<?>> {

    @NotNull <U extends Unit<?>> Set<String> vaults(@NotNull Class<U> type);

    @Nullable <U extends Unit<?>> Function<V, Vault<U>> vault(@NotNull Class<U> type, @NotNull String key);

}
