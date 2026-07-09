package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Nexo;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class NeoForgeNexoModDiscoveryHandler extends NexoModDiscoveryHandler<NeoForgeNexoMinecraft> {

    public NeoForgeNexoModDiscoveryHandler(NeoForgeNexoMinecraft nexo) {
        super(nexo);
    }

    @Override
    public void init() {
        Set<Path> jars = new LinkedHashSet<>();
        Set<Path> dirs = new LinkedHashSet<>();

        String legacyCp = System.getProperty("legacyClassPath");
        if (legacyCp != null) {
            for (String entry : legacyCp.split(File.pathSeparator)) {
                NexoModDiscoveryHandler.addPath(Path.of(entry), jars, dirs);
            }
        }

        String sysCp = System.getProperty("java.class.path");
        if (sysCp != null) {
            for (String entry : sysCp.split(File.pathSeparator)) {
                NexoModDiscoveryHandler.addPath(Path.of(entry), jars, dirs);
            }
        }

        Path modsDir = FMLPaths.MODSDIR.get();
        if (Files.isDirectory(modsDir)) {
            try (var stream = Files.list(modsDir)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".jar"))
                      .forEach(p -> NexoModDiscoveryHandler.addPath(p, jars, dirs));
            } catch (IOException ignored) {}
        }

        init(jars, dirs);
        this.nexo().modBus().addListener(this::onLoadComplete);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        try {
            ModList modList = ModList.get();

            // Replace immutable mods list with mutable copy
            List<Object> modsList = new ArrayList<>(ReflectionUtils.<List<Object>>getField(ModList.class, "mods", modList));
            ReflectionUtils.setField(ModList.class, "mods", modList, modsList);

            // Replace immutable indexedMods with mutable copy
            Map<String, Object> indexedMods = new HashMap<>(ReflectionUtils.<Map<String, Object>>getField(ModList.class, "indexedMods", modList));
            ReflectionUtils.setField(ModList.class, "indexedMods", modList, indexedMods);

            // Replace immutable sortedContainers (what getSortedMods() returns — the mod list screen reads this)
            List<Object> sortedContainers = new ArrayList<>(ReflectionUtils.<List<Object>>getField(ModList.class, "sortedContainers", modList));
            ReflectionUtils.setField(ModList.class, "sortedContainers", modList, sortedContainers);

            // Also add to sortedList (getMods() for IModInfo access)
            List<Object> sortedList = new ArrayList<>(ReflectionUtils.<List<Object>>getField(ModList.class, "sortedList", modList));
            ReflectionUtils.setField(ModList.class, "sortedList", modList, sortedList);

            for (Nexo.Mod mod : this.mods.values()) {
                NeoForgeNexoModContainer container = new NeoForgeNexoModContainer(mod);
                modsList.add(container);
                indexedMods.put(mod.value(), container);
                sortedContainers.add(container);
                sortedList.add(container.getModInfo());
                NexoMinecraft.LOGGER.info("Registered Nexo mod '{}' in NeoForge ModList", mod.value());
            }
        } catch (Exception e) {
            NexoMinecraft.LOGGER.error("Failed to register Nexo mods in NeoForge ModList", e);
        }
    }
}
