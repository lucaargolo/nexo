package dev.lucaargolo.nexo.api.language;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Language {

    @NotNull String locale();

    @Nullable String translate(@NotNull String key);

    boolean contains(@NotNull String key);

}
