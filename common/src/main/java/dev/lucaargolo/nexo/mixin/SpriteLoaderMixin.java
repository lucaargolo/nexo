package dev.lucaargolo.nexo.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import dev.lucaargolo.nexo.NexoAtlas;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(SpriteLoader.class)
public class SpriteLoaderMixin {

    @Final @Shadow
    private ResourceLocation location;

    @ModifyVariable(method = "stitch", at = @At("HEAD"), argsOnly = true)
    private List<SpriteContents> injectNexoSprites(List<SpriteContents> contents) {
        // Match this SpriteLoader's atlas against registered atlas keys
        Location atlasKey = Location.of(location.getNamespace(), location.getPath());
        Map<Location, Path> registered = NexoAtlas.getRegistered(atlasKey);

        List<SpriteContents> augmented = new ArrayList<>(contents);
        for (var entry : registered.entrySet()) {
            Location location = entry.getKey();
            Path path = entry.getValue();

            //Remove extensions in the registry
            String texPath = location.path();
            int dot = texPath.lastIndexOf('.');
            if (dot > -1) {
                texPath = texPath.substring(0, dot);
            }
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.namespace(), texPath);

            // Don't override if already present in the atlas
            boolean alreadyPresent = augmented.stream().anyMatch(c -> c.name().equals(id));
            if (alreadyPresent) {
                NexoMinecraft.LOGGER.warn("Tried to override already existing texture {}", id);
                continue;
            }

            try (InputStream in = Files.newInputStream(path)) {
                NativeImage image = NativeImage.read(in);
                FrameSize dimensions = new FrameSize(image.getWidth(), image.getHeight());
                SpriteContents spriteContents = new SpriteContents(id, dimensions, image, ResourceMetadata.EMPTY);
                augmented.add(spriteContents);
            } catch (IOException e) {
                NexoMinecraft.LOGGER.error("Failed to load Nexo atlas sprite '{}' from {}", id, path, e);
            }
        }

        return augmented;
    }
}
