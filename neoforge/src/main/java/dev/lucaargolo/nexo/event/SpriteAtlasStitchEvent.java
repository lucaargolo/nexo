package dev.lucaargolo.nexo.event;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.util.Location;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.List;
import java.util.Map;

public class SpriteAtlasStitchEvent extends Event implements IModBusEvent {

    private final Location atlas;
    private final List<Location> registered;
    private final Map<Location, byte[]> embedded;

    private Nexo nexo;

    public SpriteAtlasStitchEvent(Location atlas, List<Location> registered, Map<Location, byte[]> embedded) {
        this.atlas = atlas;
        this.registered = registered;
        this.embedded = embedded;
    }

    public Location atlas() {
        return atlas;
    }

    public List<Location> registered() {
        return registered;
    }

    public Map<Location, byte[]> embedded() {
        return embedded;
    }

    public Nexo getNexo() {
        return nexo;
    }

    public void setNexo(Nexo nexo) {
        this.nexo = nexo;
    }
}
