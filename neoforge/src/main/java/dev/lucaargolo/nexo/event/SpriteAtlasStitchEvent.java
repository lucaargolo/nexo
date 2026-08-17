package dev.lucaargolo.nexo.event;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.util.Location;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.List;

public class SpriteAtlasStitchEvent extends Event implements IModBusEvent {

    private final Location atlas;
    private final List<Material<Location>> registered;
    private final List<Material<byte[]>> embedded;

    private Nexo nexo;

    public SpriteAtlasStitchEvent(Location atlas, List<Material<Location>> registered, List<Material<byte[]>> embedded) {
        this.atlas = atlas;
        this.registered = registered;
        this.embedded = embedded;
    }

    public Location atlas() {
        return atlas;
    }

    public List<Material<Location>> registered() {
        return registered;
    }

    public List<Material<byte[]>> embedded() {
        return embedded;
    }

    public Nexo getNexo() {
        return nexo;
    }

    public void setNexo(Nexo nexo) {
        this.nexo = nexo;
    }
}
