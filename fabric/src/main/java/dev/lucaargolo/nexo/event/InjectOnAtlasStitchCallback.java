package dev.lucaargolo.nexo.event;

import dev.lucaargolo.nexo.api.util.Location;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.renderer.texture.SpriteContents;

import java.util.ArrayList;
import java.util.List;

public interface InjectOnAtlasStitchCallback {

    Event<InjectOnAtlasStitchCallback> EVENT = EventFactory.createArrayBacked(InjectOnAtlasStitchCallback.class, callbacks -> (atlas) -> {
        List<SpriteContents> list = new ArrayList<>();
        for (InjectOnAtlasStitchCallback callback : callbacks) {
            list.addAll(callback.onStitch(atlas));
        }
        return list;
    });

    List<SpriteContents> onStitch(Location atlas);

}
