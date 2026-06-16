package dev.lucaargolo.nexo;
import dev.lucaargolo.nexo.api.IMod;
import dev.lucaargolo.nexo.api.Nexo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

public abstract class NexoModDiscovery {

    private static final byte[] MOD_DESCRIPTOR = "Ldev/lucaargolo/nexo/api/IMod;".getBytes(StandardCharsets.UTF_8);
    private final Map<String, NexoMod> mods = new ConcurrentHashMap<>();

    public abstract void discover(Nexo nexo);

    public abstract void finish();

    public final Collection<NexoMod> getMods() {
        return mods.values();
    }

    protected final void discover(Nexo nexo, Collection<Path> jarPaths, Collection<Path> dirPaths) {
        ClassLoader parentCl = NexoModDiscovery.class.getClassLoader();

        List<Candidate> candidates = new ArrayList<>();
        for (Path jarPath : jarPaths) {
            scanJarForMods(jarPath, candidates);
        }
        for (Path dirPath : dirPaths) {
            scanDirectoryForMods(dirPath, candidates);
        }

        int discovered = 0;
        for (Candidate candidate : candidates) {
            Class<?> modClass = loadCandidate(candidate, parentCl);
            if (modClass == null) continue;
            IMod modAnnotation = modClass.getDeclaredAnnotation(IMod.class);
            String modId, name, description, version;
            String[] authors;
            if (modAnnotation != null) {
                modId = modAnnotation.value();
                name = modAnnotation.name();
                description = modAnnotation.description();
                version = modAnnotation.version();
                authors = modAnnotation.authors();
            } else {
                NexoMinecraft.LOGGER.warn("Bytecode scan found '{}' but @Mod annotation not readable — using class name as id", modClass.getName());
                modId = modClass.getSimpleName().toLowerCase().replace("nexo", "").replace("mod", "");
                name = modClass.getSimpleName();
                description = "";
                version = "0.0.0";
                authors = new String[0];
            }

            NexoMinecraft.LOGGER.info("Discovered Nexo mod '{}' (ID: {}, version: {})", modClass.getName(), modId, version);
            discovered++;
            mods.put(modId, new NexoMod(modId, name, description, version, authors, modClass.getName(), candidate.sourceJar));
            instantiateMod(modClass, nexo);
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
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("resource")
    private static Class<?> loadCandidate(Candidate candidate, ClassLoader classLoader) {
        try {
            if (candidate.sourceJar != null) {
                URL url = candidate.sourceJar.toUri().toURL();
                URLClassLoader jarLoader = new URLClassLoader(new URL[]{url}, classLoader);
                return jarLoader.loadClass(candidate.className);
            } else {
                return Class.forName(candidate.className, false, classLoader);
            }
        } catch (ClassNotFoundException | NoClassDefFoundError | IOException e) {
            NexoMinecraft.LOGGER.warn("Discovered Nexo mod class '{}' but failed to load it", candidate.className, e);
            return null;
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
            NexoMinecraft.LOGGER.warn("Failed to instantiate Nexo mod '{}'", modClass.getName(), e);
        }
    }

    private static void scanJarForMods(Path jarPath, List<Candidate> candidates) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class")) continue;

                byte[] classBytes;
                try (InputStream is = jar.getInputStream(entry)) {
                    classBytes = is.readAllBytes();
                }

                if (hasDescriptor(classBytes)) {
                    String className = name.substring(0, name.length() - 6).replace('/', '.');
                    if (validPackage(className)) {
                        candidates.add(new Candidate(className, jarPath));
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    private static void scanDirectoryForMods(Path dir, List<Candidate> candidates) {
        try (var stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.getFileName().toString().endsWith(".class"))
                  .forEach(p -> {
                      try {
                          byte[] classBytes = Files.readAllBytes(p);
                          if (hasDescriptor(classBytes)) {
                              String relative = dir.relativize(p).toString().replace(File.separatorChar, '.');
                              String className = relative.substring(0, relative.length() - 6);
                              if (validPackage(className)) {
                                  candidates.add(new Candidate(className, null));
                              }
                          }
                      } catch (IOException ignored) {}
                  });
        } catch (IOException ignored) {}
    }

    private static boolean hasDescriptor(byte[] haystack) {
        int max = haystack.length - MOD_DESCRIPTOR.length;
        outer:
        for (int i = 0; i <= max; i++) {
            for (int j = 0; j < MOD_DESCRIPTOR.length; j++) {
                if (haystack[i + j] != MOD_DESCRIPTOR[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private static boolean validPackage(String className) {
        int dot = className.lastIndexOf('.');
        String pkg = dot < 0 ? "" : className.substring(0, dot);
        return !pkg.startsWith("java.")
            && !pkg.startsWith("sun.")
            && !pkg.startsWith("jdk.")
            && !pkg.startsWith("com.sun.")
            && !pkg.startsWith("com.google.")
            && !pkg.startsWith("org.objectweb.")
            && !pkg.startsWith("org.apache.")
            && !pkg.startsWith("it.unimi.")
            && !pkg.startsWith("net.minecraft.")
            && !pkg.startsWith("net.fabricmc.")
            && !pkg.startsWith("net.neoforged.")
            && !pkg.startsWith("org.slf4j.")
            && !pkg.startsWith("org.jetbrains.")
            && !pkg.startsWith("org.lwjgl.")
            && !pkg.startsWith("dev.lucaargolo.nexo");
    }

    private record Candidate(String className, Path sourceJar) {}

}
