package dev.lucaargolo.nexo.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LanguageLookupCallback {

    Event<LanguageLookupCallback> EVENT = EventFactory.createArrayBacked(LanguageLookupCallback.class, callbacks -> key -> {
        for (LanguageLookupCallback callback : callbacks) {
            String translation = callback.translate(key);
            if (translation != null) {
                return translation;
            }
        }
        return null;
    });

    @Nullable String translate(@NotNull String key);

}
