package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.event.SpriteAtlasStitchEvent;
import dev.lucaargolo.nexo.render.MinecraftAtlas;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.LinkedList;
import java.util.List;

@Mixin(SpriteLoader.class)
public class SpriteLoaderMixin {

    @Final @Shadow
    private ResourceLocation location;

    @ModifyVariable(method = "stitch", at = @At("HEAD"), argsOnly = true)
    private List<SpriteContents> injectNexoSprites(List<SpriteContents> contents) {
        // Match this SpriteLoader's atlas against registered atlas keys
        Location atlasKey = Location.of(location.getNamespace(), location.getPath());
        List<Material<Location>> registered = new LinkedList<>();
        List<Material<byte[]>> embedded = new LinkedList<>();
        Nexo nexo = ModLoader.postEventWithReturn(new SpriteAtlasStitchEvent(atlasKey, registered, embedded)).getNexo();
        return MinecraftAtlas.collectSpriteContents(nexo, contents, registered, embedded);
    }
}
