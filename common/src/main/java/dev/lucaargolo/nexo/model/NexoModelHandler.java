package dev.lucaargolo.nexo.model;

import dev.lucaargolo.nexo.NexoAtlas;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.feature.IModelProvider;
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
import java.util.Map;

public abstract class NexoModelHandler {

    public abstract void init(Nexo nexo);

    protected static void registerTexture(Nexo nexo, Location texture, Location atlas) {
        Nexo.Mod mod = nexo.getMod(texture.namespace());
        if (mod == null) return;
        Path filePath = mod.path().resolve(texture.path());
        if (!Files.isRegularFile(filePath)) {
            URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(texture.path());
            if (resourceUrl != null && "file".equals(resourceUrl.getProtocol())) {
                try { filePath = Path.of(resourceUrl.toURI()); } catch (URISyntaxException ignored) {}
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

    protected static void registerModelTextures(Nexo nexo, Model model) {
        for (Location texture : model.textures().values()) {
            registerTexture(nexo, texture, NexoAtlas.BLOCK_ATLAS);
        }
    }

    protected static <T extends IFeature> void collectFeatureModels(
            Nexo nexo,
            Class<T> type,
            String modelPrefix,
            Map<ResourceLocation, NexoMinecraftModel> unbakedModels,
            FeatureModelCallback<T> callback
    ) {
        nexo.getFeatureRegistry(type).entrySet().stream()
                .filter(e -> e.getValue() instanceof IModelProvider)
                .map(e -> Map.entry(e.getKey(), (IFeature & IModelProvider) e.getValue()))
                .forEach(e -> {
                    Location featureId = e.getKey();
                    var rawFeature = e.getValue();
                    Model model = rawFeature.model();

                    registerModelTextures(nexo, model);

                    ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(
                            featureId.namespace(), modelPrefix + featureId.path()
                    );
                    unbakedModels.put(modelId, new NexoMinecraftModel(model));

                    callback.accept(featureId, (T) rawFeature, model, modelId);
                });
    }

    @FunctionalInterface
    protected interface FeatureModelCallback<T extends IFeature> {
        void accept(Location featureId, T feature, Model model, ResourceLocation modelId);
    }

}
