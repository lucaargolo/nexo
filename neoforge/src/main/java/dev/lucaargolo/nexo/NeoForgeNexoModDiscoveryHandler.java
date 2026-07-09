package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Nexo;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
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

    @SuppressWarnings("unchecked")
    private void onLoadComplete(FMLLoadCompleteEvent event) {
        try {
            ModList modList = ModList.get();

            // Replace immutable mods list with mutable copy
            Field modsField = ModList.class.getDeclaredField("mods");
            modsField.setAccessible(true);
            List<Object> modsList = (List<Object>) modsField.get(modList);
            modsList = new ArrayList<>(modsList);
            modsField.set(modList, modsList);

            // Replace immutable indexedMods with mutable copy
            Field indexedModsField = ModList.class.getDeclaredField("indexedMods");
            indexedModsField.setAccessible(true);
            Map<String, Object> indexedMods = (Map<String, Object>) indexedModsField.get(modList);
            indexedMods = new HashMap<>(indexedMods);
            indexedModsField.set(modList, indexedMods);

            // Replace immutable sortedContainers (what getSortedMods() returns — the mod list screen reads this)
            Field sortedContainersField = ModList.class.getDeclaredField("sortedContainers");
            sortedContainersField.setAccessible(true);
            List<Object> sortedContainers = (List<Object>) sortedContainersField.get(modList);
            sortedContainers = new ArrayList<>(sortedContainers);
            sortedContainersField.set(modList, sortedContainers);

            // Also add to sortedList (getMods() for IModInfo access)
            Field sortedField = ModList.class.getDeclaredField("sortedList");
            sortedField.setAccessible(true);
            List<Object> sortedList = (List<Object>) sortedField.get(modList);
            sortedList = new ArrayList<>(sortedList);
            sortedField.set(modList, sortedList);

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
