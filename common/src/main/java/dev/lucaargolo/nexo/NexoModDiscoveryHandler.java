package dev.lucaargolo.nexo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.NexoException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public abstract class NexoModDiscoveryHandler<N extends Nexo> {

    private static final String MOD_JSON = "nexo.mod.json";

    protected final Map<String, Nexo.Mod> mods = new ConcurrentHashMap<>();

    private final N nexo;

    public NexoModDiscoveryHandler(N nexo) {
        this.nexo = nexo;
    }

    public N nexo() {
        return nexo;
    }

    public abstract void init();

    public final @Nullable Nexo.Mod getMod(String id) {
        return mods.get(id);
    }

    protected final void init(Collection<Path> jarPaths, Collection<Path> dirPaths) {
        ClassLoader parentCl = NexoModDiscoveryHandler.class.getClassLoader();

        List<ModDescriptor> descriptors = new ArrayList<>();
        for (Path jarPath : jarPaths) {
            scanJarForModJson(jarPath, descriptors);
        }
        for (Path dirPath : dirPaths) {
            scanDirectoryForModJson(dirPath, descriptors);
        }

        int discovered = 0;
        for (ModDescriptor descriptor : descriptors) {
            NexoMinecraft.LOGGER.info("Discovered Nexo mod {} ({}) at {}", descriptor.id, descriptor.name, descriptor.sourcePath);
            discovered++;
            if (mods.containsKey(descriptor.id)) continue;

            Nexo.Mod mod = new Nexo.Mod(descriptor.id, descriptor.name, descriptor.description, descriptor.version, descriptor.authors, descriptor.sourcePath);
            mods.put(descriptor.id, mod);

            if (descriptor.entrypoint != null && !descriptor.entrypoint.isEmpty()) {
                instantiateMod(loadEntrypoint(descriptor, parentCl), nexo);
            }
        }

        NexoMinecraft.LOGGER.info("Nexo mod scan complete: {} entries, {} mods discovered", jarPaths.size() + dirPaths.size(), discovered);
    }

    protected static void addPath(Path path, Collection<Path> jars, Collection<Path> dirs) {
        try {
            if (Files.isDirectory(path)) {
                dirs.add(path);
            } else if (path.getFileName().toString().endsWith(".jar") && Files.exists(path)) {
                jars.add(path);
            }
        } catch (Exception ignored) {
        }
    }

    private static void scanJarForModJson(Path jarPath, List<ModDescriptor> descriptors) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry(MOD_JSON);
            if (entry != null) {
                try (InputStream is = jar.getInputStream(entry); InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    ModDescriptor descriptor = parseModDescriptor(json, jarPath);
                    descriptors.add(descriptor);
                }
            }
        } catch (IOException e) {
            NexoMinecraft.LOGGER.warn("Failed to scan JAR for {}: {}", jarPath, e.getMessage());
        }
    }

    private static void scanDirectoryForModJson(Path dirPath, List<ModDescriptor> descriptors) {
        Path jsonPath = dirPath.resolve(MOD_JSON);
        if (Files.isRegularFile(jsonPath)) {
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(jsonPath), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                ModDescriptor descriptor = parseModDescriptor(json, dirPath);
                descriptors.add(descriptor);
            } catch (IOException e) {
                NexoMinecraft.LOGGER.warn("Failed to read {}: {}", jsonPath, e.getMessage());
            }
        }
    }

    private static ModDescriptor parseModDescriptor(JsonObject json, Path sourcePath) {
        try {
            String id = json.get("id").getAsString();
            String name = json.has("name") ? json.get("name").getAsString() : "";
            String description = json.has("description") ? json.get("description").getAsString() : "";
            String version = json.has("version") ? json.get("version").getAsString() : "0.0.0";
            String entrypoint = json.has("entrypoint") ? json.get("entrypoint").getAsString() : "";

            String[] authors;
            if (json.has("authors") && json.get("authors").isJsonArray()) {
                JsonArray authorsArray = json.getAsJsonArray("authors");
                authors = new String[authorsArray.size()];
                for (int i = 0; i < authorsArray.size(); i++) {
                    authors[i] = authorsArray.get(i).getAsString();
                }
            } else {
                authors = new String[0];
            }

            return new ModDescriptor(id, name, description, version, Arrays.asList(authors), entrypoint, sourcePath);
        } catch (Exception e) {
            throw new NexoException("Failed to parse Nexo mod descriptor", e);
        }
    }

    private static Class<?> loadEntrypoint(ModDescriptor descriptor, ClassLoader parentCl) {
        try {
            if (Files.isRegularFile(descriptor.sourcePath)) {
                // JAR: create a classloader for this JAR
                URL url = descriptor.sourcePath.toUri().toURL();
                URLClassLoader jarLoader = new URLClassLoader(new URL[]{url}, parentCl);
                return jarLoader.loadClass(descriptor.entrypoint);
            } else {
                // Directory: class should be on the system classpath
                return Class.forName(descriptor.entrypoint, false, parentCl);
            }
        } catch (Exception e) {
            throw new NexoException("Failed to find Nexo mod entrypoint", e);
        }
    }

    private static void instantiateMod(Class<?> modClass, Nexo nexo) {
        try {
            try {
                Constructor<?> ctor = modClass.getDeclaredConstructor(Nexo.class);
                ctor.setAccessible(true);
                ctor.newInstance(nexo);
            } catch (NoSuchMethodException e) {
                Constructor<?> ctor = modClass.getDeclaredConstructor();
                ctor.setAccessible(true);
                ctor.newInstance();
            }
        } catch (Exception e) {
            throw new NexoException("Failed to instantiate Nexo mod", e);
        }
    }

    private record ModDescriptor(
            String id,
            String name,
            String description,
            String version,
            List<String> authors,
            String entrypoint,
            Path sourcePath
    ) {}

}
