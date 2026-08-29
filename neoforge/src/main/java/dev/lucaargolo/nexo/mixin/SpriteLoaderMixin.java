package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.event.InjectOnAtlasStitchEvent;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(SpriteLoader.class)
public class SpriteLoaderMixin {

    @Final @Shadow
    private ResourceLocation location;

    @ModifyVariable(method = "stitch", at = @At("HEAD"), argsOnly = true)
    private List<SpriteContents> injectNexoSprites(List<SpriteContents> contents) {
        Location atlasKey = Location.of(location.getNamespace(), location.getPath());
        List<SpriteContents> injected = ModLoader.postEventWithReturn(new InjectOnAtlasStitchEvent(atlasKey)).injected();
        List<SpriteContents> augmented = new ArrayList<>();
        augmented.addAll(contents);
        augmented.addAll(injected);
        return augmented;
    }

}
