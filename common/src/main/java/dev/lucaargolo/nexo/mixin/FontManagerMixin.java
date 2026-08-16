package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.render.MinecraftFont;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FontManager.class)
public abstract class FontManagerMixin {

    @Inject(method = "apply", at = @At("TAIL"))
    private void reloadRegisteredFonts(CallbackInfo callbackInfo) {
        MinecraftFont.reloadRegisteredFonts((FontManager) (Object) this);
    }

}
