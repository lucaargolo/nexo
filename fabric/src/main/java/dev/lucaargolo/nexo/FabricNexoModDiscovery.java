package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.NexoMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.FabricLoaderImpl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FabricNexoModDiscovery extends NexoModDiscovery {

    @Override
    public void init(Nexo nexo) {
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
            } catch (IOException ignored) {}
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
            } catch (IOException ignored) {}
        }

        init(nexo, jars, dirs);
        registerNexoMods();
    }

    @SuppressWarnings("unchecked")
    private void registerNexoMods() {
        try {
            FabricLoaderImpl impl = (FabricLoaderImpl) FabricLoader.getInstance();

            Field modsField = FabricLoaderImpl.class.getDeclaredField("mods");
            modsField.setAccessible(true);
            List<Object> mods = (List<Object>) modsField.get(impl);

            Field modMapField = FabricLoaderImpl.class.getDeclaredField("modMap");
            modMapField.setAccessible(true);
            Map<String, Object> modMap = (Map<String, Object>) modMapField.get(impl);

            for (NexoMod mod : this.mods.values()) {
                FabricNexoModContainer container = new FabricNexoModContainer(mod);
                mods.add(container);
                modMap.put(container.getMetadata().getId(), container);
                NexoMinecraft.LOGGER.info("Registered Nexo mod '{}' in FabricLoader internals", container.getMetadata().getId());
            }
        } catch (Exception e) {
            NexoMinecraft.LOGGER.error("Failed to register Nexo mods in FabricLoader: {}", e.getMessage());
        }
    }
}
