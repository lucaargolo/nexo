package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.event.LanguageReloadEvent;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.common.NeoForge;
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
        NeoForge.EVENT_BUS.post(new LanguageReloadEvent(manager.getSelected()));
    }

}
