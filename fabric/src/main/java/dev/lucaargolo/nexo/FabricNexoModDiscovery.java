package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.util.Utils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.FabricLoaderImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FabricNexoModDiscovery extends NexoModDiscovery<FabricNexoMinecraft> {

    public FabricNexoModDiscovery(FabricNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public void init() {
        Set<Path> jars = new LinkedHashSet<>();
        Set<Path> dirs = new LinkedHashSet<>();

        for (var container : FabricLoader.getInstance().getAllMods()) {
            for (Path path : container.getRootPaths()) {
                NexoModDiscovery.addPath(path, jars, dirs);
            }
        }

        String fabricCp = System.getProperty("fabric.remapClasspathFile");
        if (fabricCp != null) {
            try {
                for (String entry : Files.readString(Path.of(fabricCp)).split(File.pathSeparator)) {
                    NexoModDiscovery.addPath(Path.of(entry), jars, dirs);
                }
            } catch (IOException ignored) {
            }
        }

        String sysCp = System.getProperty("java.class.path");
        if (sysCp != null) {
            for (String entry : sysCp.split(File.pathSeparator)) {
                NexoModDiscovery.addPath(Path.of(entry), jars, dirs);
            }
        }

        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        if (Files.isDirectory(modsDir)) {
            try (var stream = Files.list(modsDir)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".jar"))
                      .forEach(p -> NexoModDiscovery.addPath(p, jars, dirs));
            } catch (IOException ignored) {
            }
        }

        init(jars, dirs);
        registerNexoMods();
    }

    private void registerNexoMods() {
        try {
            FabricLoaderImpl impl = (FabricLoaderImpl) FabricLoader.getInstance();

            List<Object> mods = Utils.getField(FabricLoaderImpl.class, "mods", impl);
            Map<String, Object> modMap = Utils.getField(FabricLoaderImpl.class, "modMap", impl);

            for (Nexo.Mod mod : this.mods.values()) {
                FabricNexoModContainer container = new FabricNexoModContainer(mod);
                mods.add(container);
                modMap.put(container.getMetadata().getId(), container);
                NexoMinecraft.LOGGER.info("Registered Nexo mod '{}' in FabricLoader internals", container.getMetadata().getId());
            }
        } catch (Exception e) {
            NexoMinecraft.LOGGER.error("Failed to register Nexo mods in FabricLoader", e);
        }
    }
}
