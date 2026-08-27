package dev.lucaargolo.nexo.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.jetbrains.annotations.NotNull;

public interface LanguageReloadCallback {

    Event<LanguageReloadCallback> EVENT = EventFactory.createArrayBacked(
            LanguageReloadCallback.class,
            callbacks -> locale -> {
                for (LanguageReloadCallback callback : callbacks) {
                    callback.onReload(locale);
                }
            }
    );

    void onReload(@NotNull String locale);

}
