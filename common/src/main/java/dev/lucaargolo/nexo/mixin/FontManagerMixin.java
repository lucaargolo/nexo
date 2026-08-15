package dev.lucaargolo.nexo.mixin;

import com.mojang.blaze3d.font.GlyphProvider;
import dev.lucaargolo.nexo.mixed.MinecraftFontManagerMixed;
import dev.lucaargolo.nexo.resource.font.MinecraftTTFFontResource;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(FontManager.class)
public abstract class FontManagerMixin implements MinecraftFontManagerMixed {

    @Shadow
    @Final
    private TextureManager textureManager;

    @Shadow
    @Final
    private List<GlyphProvider> providersToClose;

    @Shadow
    @Final
    private Map<ResourceLocation, FontSet> fontSets;

    @Shadow
    private @Nullable volatile FontSet lastFontSetCache;

    @Override
    public TextureManager nexo$getTextureManager() {
        return textureManager;
    }

    @Override
    public List<GlyphProvider> nexo$getProvidersToClose() {
        return providersToClose;
    }

    @Override
    public Map<ResourceLocation, FontSet> nexo$getFontSets() {
        return fontSets;
    }

    @Override
    public void nexo$clearLastFontSetCache() {
        lastFontSetCache = null;
    }

    @Inject(method = "apply", at = @At("TAIL"))
    private void reloadRegisteredFonts(CallbackInfo callbackInfo) {
        MinecraftTTFFontResource.reloadRegisteredFonts((FontManager) (Object) this);
    }

}
