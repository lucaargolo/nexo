package dev.lucaargolo.nexo.event;

import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public class AtlasStitchedEvent extends Event implements IModBusEvent {

    private final TextureAtlas atlas;
    private final SpriteLoader.Preparations preparations;

    public AtlasStitchedEvent(TextureAtlas atlas, SpriteLoader.Preparations preparations) {
        this.atlas = atlas;
        this.preparations = preparations;
    }

    public TextureAtlas atlas() {
        return atlas;
    }

    public SpriteLoader.Preparations preparations() {
        return preparations;
    }

}
