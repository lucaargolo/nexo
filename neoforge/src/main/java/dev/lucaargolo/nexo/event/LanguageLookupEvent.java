package dev.lucaargolo.nexo.event;

import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LanguageLookupEvent extends Event {

    private final @NotNull String key;
    private @Nullable String translation;

    public LanguageLookupEvent(@NotNull String key) {
        this.key = key;
    }

    public @NotNull String key() {
        return key;
    }

    public @Nullable String translation() {
        return translation;
    }

    public void translation(@Nullable String translation) {
        this.translation = translation;
    }

}
