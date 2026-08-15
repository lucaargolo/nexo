package dev.lucaargolo.nexo.mixed;

import com.mojang.blaze3d.font.GlyphProvider;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public interface MinecraftFontManagerMixed {

    TextureManager nexo$getTextureManager();

    List<GlyphProvider> nexo$getProvidersToClose();

    Map<ResourceLocation, FontSet> nexo$getFontSets();

    void nexo$clearLastFontSetCache();

}
