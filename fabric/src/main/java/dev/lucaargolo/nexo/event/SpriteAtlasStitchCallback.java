package dev.lucaargolo.nexo.event;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.util.Location;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.List;
import java.util.Map;

public interface SpriteAtlasStitchCallback {

    Event<SpriteAtlasStitchCallback> EVENT = EventFactory.createArrayBacked(SpriteAtlasStitchCallback.class, callbacks -> (atlas, registered, embedded) -> {
        Nexo nexo = null;
        for (SpriteAtlasStitchCallback callback : callbacks) {
            nexo = callback.onStitch(atlas, registered, embedded);
        }
        return nexo;
    });

    Nexo onStitch(Location atlas, List<Location> registered, Map<Location, byte[]> embedded);

}
