package dev.lucaargolo.nexo;

import com.mojang.blaze3d.platform.NativeImage;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.image.ImageResource;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class NexoAtlas {

    public static final Location BLOCK_ATLAS = Location.of("minecraft", "textures/atlas/blocks.png");

    private final Map<Location, List<Location>> registry = new ConcurrentHashMap<>();
    private final Map<Location, Map<Location, byte[]>> embeddedRegistry = new ConcurrentHashMap<>();

    public void register(@NotNull Location atlas, @NotNull Location texture) {
        registry.computeIfAbsent(atlas, k -> new CopyOnWriteArrayList<>()).add(texture);
    }

    public void register(@NotNull Location atlas, @NotNull Location texture, byte @NotNull [] data) {
        embeddedRegistry.computeIfAbsent(atlas, k -> new ConcurrentHashMap<>()).put(texture, data.clone());
    }

    public @NotNull List<Location> getRegistered(@NotNull Location atlas) {
        return registry.getOrDefault(atlas, List.of());
    }

    public @NotNull Map<Location, byte[]> getEmbedded(@NotNull Location atlas) {
        return embeddedRegistry.getOrDefault(atlas, Map.of());
    }

    public static @NotNull List<SpriteContents> collectSpriteContents(Nexo nexo, List<SpriteContents> contents, List<Location> registered, Map<Location, byte[]> embedded) {
        List<SpriteContents> augmented = new ArrayList<>(contents);
        for (Location location : registered) {
            ResourceLocation id = NexoMinecraft.rl(location);
            boolean alreadyPresent = augmented.stream().anyMatch(c -> c.name().equals(id));
            if (alreadyPresent) {
                continue;
            }

            try {
                ImageResource.PNG resource = nexo.getResource(Resource.Type.PNG_IMAGE, location);
                if (resource != null) {
                    NativeImage image = NativeImage.read(resource.data());
                    FrameSize dimensions = new FrameSize(image.getWidth(), image.getHeight());
                    SpriteContents spriteContents = new SpriteContents(id, dimensions, image, ResourceMetadata.EMPTY);
                    augmented.add(spriteContents);
                    NexoMinecraft.LOGGER.debug("Injected {} at {}", spriteContents, id);
                } else {
                    throw new FileNotFoundException();
                }
            } catch (Exception e) {
                NexoMinecraft.LOGGER.error("Failed to load Nexo atlas sprite '{}'", location, e);
            }
        }

        for (Map.Entry<Location, byte[]> entry : embedded.entrySet()) {
            Location location = entry.getKey().withoutExtension();
            ResourceLocation id = NexoMinecraft.rl(location);
            boolean alreadyPresent = augmented.stream().anyMatch(c -> c.name().equals(id));
            if (alreadyPresent) {
                NexoMinecraft.LOGGER.warn("Tried to override already existing texture {}", id);
                continue;
            }

            try (InputStream in = new ByteArrayInputStream(entry.getValue())) {
                NativeImage image = NativeImage.read(in);
                FrameSize dimensions = new FrameSize(image.getWidth(), image.getHeight());
                augmented.add(new SpriteContents(id, dimensions, image, ResourceMetadata.EMPTY));
            } catch (IOException e) {
                NexoMinecraft.LOGGER.error("Failed to load embedded Nexo atlas sprite '{}'", id, e);
            }
        }
        return augmented;
    }

}
