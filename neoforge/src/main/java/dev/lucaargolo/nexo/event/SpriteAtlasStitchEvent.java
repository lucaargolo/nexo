package dev.lucaargolo.nexo.event;

import dev.lucaargolo.nexo.api.util.Location;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.nio.file.Path;
import java.util.Map;

public class SpriteAtlasStitchEvent extends Event implements IModBusEvent {

    private final Location atlas;
    private final Map<Location, Path> registered;
    private final Map<Location, byte[]> embedded;

    public SpriteAtlasStitchEvent(Location atlas, Map<Location, Path> registered, Map<Location, byte[]> embedded) {
        this.atlas = atlas;
        this.registered = registered;
        this.embedded = embedded;
    }

    public Location atlas() {
        return atlas;
    }

    public Map<Location, Path> registered() {
        return registered;
    }

    public Map<Location, byte[]> embedded() {
        return embedded;
    }

}
