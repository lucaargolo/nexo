package dev.lucaargolo.nexo.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.util.LayerMode;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.image.ImageResource;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.api.util.Pair;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MinecraftAtlas {

    public static final Location BLOCK_ATLAS = Location.of("minecraft", "textures/atlas/blocks.png");

    private final Map<Location, List<Material<Location>>> registry = new ConcurrentHashMap<>();
    private final Map<Location, List<Material<byte[]>>> embeddedRegistry = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public void register(Location atlas, Material<?> material) {
        Pair<Location, ?> textureData = material.texture();
        if (textureData == null) {
            return;
        }
        Object data = textureData.right();
        if(data instanceof Location) {
            registry.computeIfAbsent(atlas, k -> new CopyOnWriteArrayList<>()).add((Material<Location>) material);
        }else if(data instanceof byte[]) {
            embeddedRegistry.computeIfAbsent(atlas, k -> new CopyOnWriteArrayList<>()).add((Material<byte[]>) material);
        }
    }

    public @NotNull List<Material<Location>> getRegistered(@NotNull Location atlas) {
        return registry.getOrDefault(atlas, List.of());
    }

    public @NotNull List<Material<byte[]>> getEmbedded(@NotNull Location atlas) {
        return embeddedRegistry.getOrDefault(atlas, List.of());
    }

    public static @NotNull List<SpriteContents> collectSpriteContents(Nexo nexo, List<SpriteContents> contentsList, List<Material<Location>> registered, List<Material<byte[]>> embedded) {
        List<SpriteContents> augmented = new ArrayList<>(contentsList);
        Map<ResourceLocation, SpriteContents> existing = new HashMap<>(contentsList.size());
        for (SpriteContents contents : contentsList) {
            existing.put(contents.name(), contents);
        }

        for (Material<Location> material : registered) {
            Pair<Location, Location> texture = material.texture();
            if (texture == null) {
                continue;
            }
            Location location = texture.left();
            ResourceLocation id = NexoMinecraft.rl(location.withoutExtension());
            try {
                ImageResource resource = nexo.getResource(Resource.Type.IMAGE, location);
                if (resource != null) {
                    NativeImage image = NativeImage.read(resource.data());
                    classify(material, image);
                    FrameSize dimensions = new FrameSize(image.getWidth(), image.getHeight());
                    SpriteContents spriteContents = new SpriteContents(id, dimensions, image, ResourceMetadata.EMPTY);
                    augmented.add(spriteContents);
                    NexoMinecraft.LOGGER.debug("Injected {} at {}", spriteContents, id);
                } else {
                    SpriteContents contents = existing.get(id);
                    if (contents == null) {
                        throw new FileNotFoundException();
                    }
                    classify(material, contents.originalImage);
                }
            } catch (Exception e) {
                NexoMinecraft.LOGGER.error("Failed to load Nexo atlas sprite '{}'", location, e);
            }
        }

        for (Material<byte[]> material : embedded) {
            Pair<Location, byte[]> texture = material.texture();
            if (texture == null) {
                continue;
            }
            ResourceLocation id = NexoMinecraft.rl(texture.left().withoutExtension());
            try (InputStream in = new ByteArrayInputStream(texture.right())) {
                NativeImage image = NativeImage.read(in);
                classify(material, image);
                FrameSize dimensions = new FrameSize(image.getWidth(), image.getHeight());
                augmented.add(new SpriteContents(id, dimensions, image, ResourceMetadata.EMPTY));
            } catch (IOException e) {
                NexoMinecraft.LOGGER.error("Failed to load embedded Nexo atlas sprite '{}'", id, e);
            }
        }
        return augmented;
    }

    private static void classify(Material<?> material, NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        LayerMode inferred = LayerMode.SOLID;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = image.getLuminanceOrAlpha(x, y) & 0xFF;

                if (alpha > 0 && alpha < 255) {
                    inferred = LayerMode.TRANSLUCENT;
                    break;
                }else if(alpha == 0) {
                    inferred = LayerMode.CUTOUT;
                }
            }
        }

        if (inferred.ordinal() > material.layerMode().ordinal()) {
            material.withLayerMode(inferred);
        }
    }

}
