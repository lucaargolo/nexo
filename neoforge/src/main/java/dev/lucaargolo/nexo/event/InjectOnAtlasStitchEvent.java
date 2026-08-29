package dev.lucaargolo.nexo.event;

import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.ArrayList;
import java.util.List;

public class InjectOnAtlasStitchEvent extends Event implements IModBusEvent {

    private final List<SpriteContents> injected = new ArrayList<>();

    private final Location atlas;

    public InjectOnAtlasStitchEvent(Location atlas) {
        this.atlas = atlas;
    }

    public List<SpriteContents> injected() {
        return injected;
    }

    public Location atlas() {
        return atlas;
    }

}
