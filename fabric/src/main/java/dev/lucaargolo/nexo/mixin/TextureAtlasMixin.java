package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.event.AtlasStitchedCallback;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin {

    @Inject(method = "upload", at = @At("RETURN"))
    private void nexo$onAtlasStitched(SpriteLoader.Preparations preparations, CallbackInfo callbackInfo) {
        AtlasStitchedCallback.EVENT.invoker().onStitched((TextureAtlas) (Object) this, preparations);
    }

}
