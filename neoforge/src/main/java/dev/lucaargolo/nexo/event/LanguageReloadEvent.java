package dev.lucaargolo.nexo.event;

import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

public final class LanguageReloadEvent extends Event {

    private final @NotNull String locale;

    public LanguageReloadEvent(@NotNull String locale) {
        this.locale = locale;
    }

    public @NotNull String locale() {
        return locale;
    }

}
