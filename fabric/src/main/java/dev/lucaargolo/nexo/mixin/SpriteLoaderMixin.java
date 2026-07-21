package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.NexoAtlas;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.event.SpriteAtlasStitchCallback;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Mixin(SpriteLoader.class)
public class SpriteLoaderMixin {

    @Final @Shadow
    private ResourceLocation location;

    @ModifyVariable(method = "stitch", at = @At("HEAD"), argsOnly = true)
    private List<SpriteContents> injectNexoSprites(List<SpriteContents> contents) {
        Location atlasKey = Location.of(location.getNamespace(), location.getPath());
        List<Location> registered = new LinkedList<>();
        Map<Location, byte[]> embedded = new LinkedHashMap<>();
        Nexo nexo = SpriteAtlasStitchCallback.EVENT.invoker().onStitch(atlasKey, registered, embedded);
        return NexoAtlas.collectSpriteContents(nexo, contents, registered, embedded);
    }


}
