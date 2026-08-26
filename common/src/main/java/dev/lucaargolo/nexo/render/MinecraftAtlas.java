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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

public final class MinecraftAtlas implements PreparableReloadListener {
    public static final Location BLOCK_ATLAS = Location.of("minecraft", "textures/atlas/blocks.png");
    public static final Location ENTITY_ATLAS = Location.of("nexo", "textures/atlas/entity.png");
    public static final Location SCREEN_ATLAS = Location.of("minecraft", "textures/atlas/gui.png");

    private final Map<Location, List<Material<Location>>> registry = new ConcurrentHashMap<>();
    private final Map<Location, List<Material<byte[]>>> embeddedRegistry = new ConcurrentHashMap<>();
    private final Map<Location, TextureAtlas> managedAtlases = new ConcurrentHashMap<>();

    @Override
    public @NotNull CompletableFuture<Void> reload(
            @NotNull PreparationBarrier barrier,
            @NotNull ResourceManager resourceManager,
            @NotNull ProfilerFiller preparationsProfiler,
            @NotNull ProfilerFiller reloadProfiler,
            @NotNull Executor backgroundExecutor,
            @NotNull Executor gameExecutor
    ) {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        Set<Location> locations = new HashSet<>(registry.keySet());
        locations.addAll(embeddedRegistry.keySet());
        List<CompletableFuture<Void>> reloads = new ArrayList<>();
        for (Location location : locations) {
            ResourceLocation atlasLocation = NexoMinecraft.rl(location);
            AbstractTexture existing = textureManager.getTexture(atlasLocation, null);
            TextureAtlas atlas;
            if (existing instanceof TextureAtlas existingAtlas) {
                if (managedAtlases.get(location) != existingAtlas) {
                    continue;
                }
                atlas = existingAtlas;
            } else {
                atlas = new TextureAtlas(atlasLocation);
                textureManager.register(atlasLocation, atlas);
                managedAtlases.put(location, atlas);
            }
            reloads.add(
                    SpriteLoader.create(atlas)
                            .loadAndStitch(
                                    resourceManager,
                                    NexoMinecraft.rl(atlasInfo(location)),
                                    0,
                                    backgroundExecutor
                            )
                            .thenCompose(SpriteLoader.Preparations::waitForUpload)
                            .thenCompose(barrier::wait)
                            .thenAcceptAsync(atlas::upload, gameExecutor)
            );
        }
        return CompletableFuture.allOf(reloads.toArray(CompletableFuture[]::new));
    }

    public static @NotNull Location atlasInfo(@NotNull Location atlas) {
        String path = atlas.withoutExtension().path();
        String prefix = "textures/atlas/";
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        return Location.of(atlas.namespace(), "atlases/" + path);
    }

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
