package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Nexo;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public class NeoForgeNexoModDiscovery extends NexoModDiscovery {

    @Override
    public void discover(Nexo nexo) {
        Set<Path> jars = new LinkedHashSet<>();
        Set<Path> dirs = new LinkedHashSet<>();

        String legacyCp = System.getProperty("legacyClassPath");
        if (legacyCp != null) {
            for (String entry : legacyCp.split(File.pathSeparator)) {
                NexoModDiscovery.addPath(Path.of(entry), jars, dirs);
            }
        }

        String sysCp = System.getProperty("java.class.path");
        if (sysCp != null) {
            for (String entry : sysCp.split(File.pathSeparator)) {
                NexoModDiscovery.addPath(Path.of(entry), jars, dirs);
            }
        }

        Path modsDir = FMLPaths.MODSDIR.get();
        if (Files.isDirectory(modsDir)) {
            try (var stream = Files.list(modsDir)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".jar"))
                      .forEach(p -> NexoModDiscovery.addPath(p, jars, dirs));
            } catch (IOException ignored) {}
        }

        discover(nexo, jars, dirs);
    }
}
