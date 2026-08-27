package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.event.LanguageReloadCallback;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LanguageManager.class)
public abstract class LanguageManagerMixin {

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void selectNexoLanguage(@NotNull ResourceManager resourceManager, CallbackInfo callbackInfo) {
        LanguageManager manager = (LanguageManager) (Object) this;
        LanguageReloadCallback.EVENT.invoker().onReload(manager.getSelected());
    }

}
