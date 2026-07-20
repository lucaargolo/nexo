package dev.lucaargolo.nexo.event;

import dev.lucaargolo.nexo.api.util.Location;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.nio.file.Path;
import java.util.Map;

public interface SpriteAtlasStitchCallback {

    Event<SpriteAtlasStitchCallback> EVENT = EventFactory.createArrayBacked(SpriteAtlasStitchCallback.class, callbacks -> (atlas, registered, embedded) -> {
        for (SpriteAtlasStitchCallback callback : callbacks) {
            callback.onStitch(atlas, registered, embedded);
        }
    });

    void onStitch(Location atlas, Map<Location, Path> registered, Map<Location, byte[]> embedded);

}
