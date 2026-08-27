package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.event.LanguageLookupCallback;
import dev.lucaargolo.nexo.language.MinecraftLanguageConversions;
import dev.lucaargolo.nexo.language.MinecraftLanguageHandler;
import net.minecraft.locale.Language;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Language.class)
public abstract class LanguageMixin {

    @Inject(method = "getOrDefault(Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private void exposeNexoTranslations(@NotNull String key, CallbackInfoReturnable<String> callbackInfo) {
        if (MinecraftLanguageHandler.isMinecraftLookup()) {
            return;
        }
        String translated = LanguageLookupCallback.EVENT.invoker().translate(key);
        if (translated != null) {
            callbackInfo.setReturnValue(MinecraftLanguageConversions.nexoToMinecraft(translated));
        }
    }

}
