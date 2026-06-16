package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Nexo;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public class FabricNexoModDiscovery extends NexoModDiscovery {

    @Override
    public void discover(Nexo nexo) {
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

        discover(nexo, jars, dirs);
    }
}
