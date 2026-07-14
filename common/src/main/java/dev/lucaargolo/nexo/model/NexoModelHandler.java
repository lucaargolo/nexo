package dev.lucaargolo.nexo.model;

import dev.lucaargolo.nexo.NexoAtlas;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.event.Event;
import dev.lucaargolo.nexo.api.event.FeatureRegisteredEvent;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.ModelProvider;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class NexoModelHandler<N extends Nexo> {

    private final N nexo;

    public NexoModelHandler(N nexo) {
        this.nexo = nexo;
    }

    public N nexo() {
        return nexo;
    }

    public void init() {
        nexo.on(FeatureRegisteredEvent.class, Event.Priority.NORMAL, event -> {
            Feature<?> feature = event.value();
            if (feature instanceof ModelProvider modelProvider) {
                Model model = modelProvider.model();
                if (model != null) {
                    for (Location texture : model.textures().values()) {
                        registerTexture(nexo, texture, NexoAtlas.BLOCK_ATLAS);
                    }

                    String prefix = modelPrefix(feature);
                    ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(
                            event.location().namespace(), prefix + event.location().path()
                    );
                    NexoMinecraftModel mcModel = new NexoMinecraftModel(model);

                    collectModel(feature, model, modelId, mcModel);
                }
            }
            return true;
        });
    }

    protected abstract void collectModel(Feature<?> feature, Model model, ResourceLocation modelId, NexoMinecraftModel mcModel);

    private static String modelPrefix(Feature<?> feature) {
        if (feature instanceof BlockBase) return "block/";
        if (feature instanceof ItemBase) return "item/";
        return "";
    }

    private static void registerTexture(Nexo nexo, Location texture, Location atlas) {
        Nexo.Mod mod = nexo.getMod(texture.namespace());
        if (mod == null) return;
        Path filePath = mod.path().resolve(texture.path());
        if (!Files.isRegularFile(filePath)) {
            URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(texture.path());
            if (resourceUrl != null && "file".equals(resourceUrl.getProtocol())) {
                try {
                    filePath = Path.of(resourceUrl.toURI());
                } catch (URISyntaxException ignored) {
                }
            }
        }
        if (Files.isRegularFile(filePath)) {
            NexoAtlas.register(atlas, texture, filePath);
        } else {
            try {
                FileSystem jarFs = FileSystems.newFileSystem(mod.path(), (ClassLoader) null);
                Path jarPath = jarFs.getPath(texture.path());
                if (Files.isRegularFile(jarPath)) {
                    NexoAtlas.register(atlas, texture, jarPath);
                }
            } catch (IOException e) {
                NexoMinecraft.LOGGER.error("Failed to read from JAR {}", mod.path(), e);
            }
        }
    }

}
