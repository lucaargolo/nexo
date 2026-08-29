package dev.lucaargolo.nexo.render.atlas;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.NativeImage;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.util.LayerMode;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.image.ImageResource;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.api.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

public final class MinecraftAtlasHandler implements PreparableReloadListener {

    public static final Location BLOCK_ATLAS = Location.of("minecraft", "textures/atlas/blocks.png");
    public static final Location ENTITY_ATLAS = Location.of("nexo", "textures/atlas/entity.png");
    public static final Location SCREEN_ATLAS = Location.of("minecraft", "textures/atlas/gui.png");

    private final Map<Location, List<Material<Location>>> atlasRegistry = new ConcurrentHashMap<>();
    private final Map<Location, List<Material<byte[]>>> atlasEmbeddedRegistry = new ConcurrentHashMap<>();
    private final Map<Location, Location> atlasLookup = new LinkedHashMap<>();

    private final Map<Location, NativeImage> imagesToRegister = new LinkedHashMap<>();

    private final NexoMinecraft<?, ?, ?, ?> nexo;

    public MinecraftAtlasHandler(NexoMinecraft<?, ?, ?, ?> nexo) {
        this.nexo = nexo;
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(
            @NotNull PreparationBarrier barrier,
            @NotNull ResourceManager resourceManager,
            @NotNull ProfilerFiller preparationsProfiler,
            @NotNull ProfilerFiller reloadProfiler,
            @NotNull Executor backgroundExecutor,
            @NotNull Executor gameExecutor
    ) {
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        imagesToRegister.forEach((location, image) -> {
            manager.register(NexoMinecraft.rl(location), new DynamicTexture(image));
        });

        Set<Location> atlasesLocations = Sets.union(atlasRegistry.keySet(), atlasEmbeddedRegistry.keySet());
        List<CompletableFuture<Void>> reloads = new ArrayList<>();

        for (Location atlasLocation : atlasesLocations) {
            AbstractTexture texture = manager.getTexture(NexoMinecraft.rl(atlasLocation), null);

            if (texture == null) {
                ResourceLocation rl = NexoMinecraft.rl(atlasLocation);
                TextureAtlas atlas = new TextureAtlas(rl);
                manager.register(rl, atlas);

                CompletableFuture<Void> loader = SpriteLoader.create(atlas)
                        .loadAndStitch(resourceManager, atlas.location(), 0, backgroundExecutor)
                        .thenCompose(SpriteLoader.Preparations::waitForUpload)
                        .thenCompose(barrier::wait)
                        .thenAcceptAsync(atlas::upload, gameExecutor);

                reloads.add(loader);
            }
        }
        return CompletableFuture.allOf(reloads.toArray(CompletableFuture[]::new));
    }

    @SuppressWarnings("unchecked")
    public void register(Location atlas, Material<?> material) {
        Pair<Location, ?> textureData = material.texture();
        if (textureData == null) {
            return;
        }
        Object data = textureData.right();
        if (data instanceof Location) {
            atlasRegistry.computeIfAbsent(atlas, k -> new CopyOnWriteArrayList<>()).add((Material<Location>) material);
        } else if (data instanceof byte[]) {
            atlasEmbeddedRegistry.computeIfAbsent(atlas, k -> new CopyOnWriteArrayList<>()).add((Material<byte[]>) material);
        }
    }

    public void register(ImageResource resource) {
        try {
            NativeImage image = NativeImage.read(resource.data());
            imagesToRegister.put(resource.location(), image);
        } catch (IOException e) {
            NexoMinecraft.LOGGER.error("Failed to load Nexo image '{}'", resource.location(), e);
        }
    }

    public @Nullable Location findAtlas(@NotNull Location texture) {
        return atlasLookup.get(texture.withoutExtension());
    }

    public @NotNull List<SpriteContents> getSpriteContents(@NotNull Location atlas) {
        List<SpriteContents> list = new ArrayList<>();

        for (Material<Location> material : atlasRegistry.getOrDefault(atlas, List.of())) {
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
                    list.add(spriteContents);
                    NexoMinecraft.LOGGER.debug("Injected {} at {}", spriteContents, id);
                }
            } catch (Exception e) {
                NexoMinecraft.LOGGER.error("Failed to load Nexo atlas sprite '{}'", location, e);
            }
        }

        for (Material<byte[]> material : atlasEmbeddedRegistry.getOrDefault(atlas, List.of())) {
            Pair<Location, byte[]> texture = material.texture();
            if (texture == null) {
                continue;
            }
            ResourceLocation id = NexoMinecraft.rl(texture.left().withoutExtension());
            try (InputStream in = new ByteArrayInputStream(texture.right())) {
                NativeImage image = NativeImage.read(in);
                classify(material, image);
                FrameSize dimensions = new FrameSize(image.getWidth(), image.getHeight());
                list.add(new SpriteContents(id, dimensions, image, ResourceMetadata.EMPTY));
            } catch (IOException e) {
                NexoMinecraft.LOGGER.error("Failed to load embedded Nexo atlas sprite '{}'", id, e);
            }
        }

        return list;
    }

    public void onAtlasStitched(@NotNull TextureAtlas atlas, @NotNull SpriteLoader.Preparations preparations) {
        Location atlasLocation = Location.of(atlas.location().getNamespace(), atlas.location().getPath());
        for (ResourceLocation texture : preparations.regions().keySet()) {
            atlasLookup.putIfAbsent(Location.of(texture.getNamespace(), texture.getPath()), atlasLocation);
        }
    }

    private static void classify(Material<?> material, NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        LayerMode inferred = LayerMode.SOLID;
        scan:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = image.getLuminanceOrAlpha(x, y) & 0xFF;

                if (alpha > 0 && alpha < 255) {
                    inferred = LayerMode.TRANSLUCENT;
                    break scan;
                } else if (alpha == 0) {
                    inferred = LayerMode.CUTOUT;
                }
            }
        }

        if (inferred.ordinal() > material.layerMode().ordinal()) {
            material.withLayerMode(inferred);
        }
    }

}
