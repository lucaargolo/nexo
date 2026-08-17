package dev.lucaargolo.nexo.event;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.util.Location;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.List;

public interface SpriteAtlasStitchCallback {

    Event<SpriteAtlasStitchCallback> EVENT = EventFactory.createArrayBacked(SpriteAtlasStitchCallback.class, callbacks -> (atlas, registered, embedded) -> {
        Nexo nexo = null;
        for (SpriteAtlasStitchCallback callback : callbacks) {
            Nexo value = callback.onStitch(atlas, registered, embedded);
            if (nexo == null && value != null) {
                nexo = value;
            }
        }
        return nexo;
    });

    Nexo onStitch(Location atlas, List<Material<Location>> registered, List<Material<byte[]>> embedded);

}
