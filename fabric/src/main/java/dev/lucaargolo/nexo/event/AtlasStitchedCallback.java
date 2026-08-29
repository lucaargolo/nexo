package dev.lucaargolo.nexo.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;

public interface AtlasStitchedCallback {

    Event<AtlasStitchedCallback> EVENT = EventFactory.createArrayBacked(
            AtlasStitchedCallback.class,
            callbacks -> (atlas, preparations) -> {
                for (AtlasStitchedCallback callback : callbacks) {
                    callback.onStitched(atlas, preparations);
                }
            }
    );

    void onStitched(TextureAtlas atlas, SpriteLoader.Preparations preparations);

}
