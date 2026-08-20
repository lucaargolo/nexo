package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.render.MinecraftFont;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlyphRenderTypes.class)
public abstract class GlyphRenderTypesMixin {

    @Inject(method = "createForIntensityTexture", at = @At("RETURN"), cancellable = true)
    private static void useLinearFiltering(
            ResourceLocation fontId,
            CallbackInfoReturnable<GlyphRenderTypes> callbackInfo
    ) {
        if (MinecraftFont.useLinearFiltering(fontId)) {
            callbackInfo.setReturnValue(MinecraftFont.linearFilteringRenderTypes(callbackInfo.getReturnValue()));
        }
    }

}
