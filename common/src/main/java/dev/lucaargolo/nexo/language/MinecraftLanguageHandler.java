package dev.lucaargolo.nexo.language;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.language.Language;
import dev.lucaargolo.nexo.api.resource.language.LanguageResource;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.resource.language.MinecraftLanguageResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class MinecraftLanguageHandler implements Language {

    private static final @NotNull String DEFAULT_LOCALE = "en_us";
    private static final ThreadLocal<Boolean> MINECRAFT_LOOKUP = ThreadLocal.withInitial(() -> false);
    private static final @NotNull List<String> PATHS = List.of(
            "%s.json",
            "lang/%s.json",
            "language/%s.json",
            "languages/%s.json"
    );

    private final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;
    private final @NotNull Map<Location, LanguageResource> registered = new LinkedHashMap<>();
    private volatile @NotNull String locale = DEFAULT_LOCALE;
    private volatile @NotNull Map<String, String> entries = Map.of();

    public MinecraftLanguageHandler(@NotNull NexoMinecraft<?, ?, ?, ?> nexo) {
        this.nexo = nexo;
    }

    @Override
    public @NotNull String locale() {
        return locale;
    }

    @Override
    public @Nullable String translate(@NotNull String key) {
        String value = translateNexo(key);
        if (value != null) {
            return value;
        }
        value = minecraftTranslation(key);
        return value == null ? null : MinecraftLanguageConversions.minecraftToNexo(value);
    }

    public @Nullable String translateNexo(@NotNull String key) {
        String value = entries.get(key);
        synchronized (registered) {
            List<String> fallbacks = fallbackLocales(locale);
            for (int index = fallbacks.size() - 1; index >= 0; index--) {
                String fallback = fallbacks.get(index);
                for (LanguageResource resource : registered.values()) {
                    if (!resource.locale().equals(fallback)) {
                        continue;
                    }
                    String override = resource.entry(key);
                    if (override != null) {
                        value = override;
                    }
                }
            }
        }
        return value;
    }

    public static boolean isMinecraftLookup() {
        return MINECRAFT_LOOKUP.get();
    }

    private @Nullable String minecraftTranslation(@NotNull String key) {
        boolean previous = MINECRAFT_LOOKUP.get();
        MINECRAFT_LOOKUP.set(true);
        try {
            net.minecraft.locale.Language language = net.minecraft.locale.Language.getInstance();
            return language.has(key) ? language.getOrDefault(key) : null;
        } finally {
            MINECRAFT_LOOKUP.set(previous);
        }
    }

    @Override
    public boolean contains(@NotNull String key) {
        return translate(key) != null;
    }

    public void register(@NotNull LanguageResource resource) {
        synchronized (registered) {
            registered.put(resource.location(), resource);
        }
    }

    public void load(@NotNull Nexo.Mod mod) {
        if (NexoMinecraft.MOD_ID.equals(mod.value())) {
            return;
        }
        Map<String, String> loaded = new LinkedHashMap<>(entries());
        loadPackagedLocale(mod, DEFAULT_LOCALE, loaded);
        entries = Map.copyOf(loaded);
    }

    public void select(@NotNull String selectedLocale) {
        String selected = normalize(selectedLocale);
        List<String> fallbacks = fallbackLocales(selected);
        Map<String, String> loaded = new LinkedHashMap<>();
        for (int index = fallbacks.size() - 1; index >= 0; index--) {
            String fallback = fallbacks.get(index);
            for (Nexo.Mod mod : nexo.getDiscoveryHandler().getMods()) {
                loadPackagedLocale(mod, fallback, loaded);
            }
        }
        synchronized (registered) {
            for (int index = fallbacks.size() - 1; index >= 0; index--) {
                String fallback = fallbacks.get(index);
                for (LanguageResource resource : registered.values()) {
                    if (resource.locale().equals(fallback)) {
                        loaded.putAll(resource.entries());
                    }
                }
            }
        }
        locale = selected;
        entries = Map.copyOf(loaded);
        nexo.getLogger().debug("Selected Nexo language {} with {} translations", selected, entries.size());
    }

    public @NotNull Map<String, String> entries() {
        return entries;
    }

    private void loadPackagedLocale(
            @NotNull Nexo.Mod mod,
            @NotNull String locale,
            @NotNull Map<String, String> loaded
    ) {
        if (NexoMinecraft.MOD_ID.equals(mod.value())) {
            return;
        }
        for (String path : paths(locale)) {
            Location location = Location.of(mod.value(), path);
            byte[] data = nexo.loadResource(location);
            if (data == null) {
                continue;
            }
            try {
                LanguageResource language = MinecraftLanguageResource.parse(location, data);
                loaded.putAll(language.entries());
                nexo.getLogger().debug("Loaded Nexo language resource {}", location);
            } catch (RuntimeException exception) {
                nexo.getLogger().warn("Could not load Nexo language resource {}", location, exception);
            }
            break;
        }
    }

    private static @NotNull List<String> paths(@NotNull String locale) {
        List<String> paths = new ArrayList<>(PATHS.size());
        for (String path : PATHS) {
            paths.add(String.format(Locale.ROOT, path, locale));
        }
        return paths;
    }

    private static @NotNull List<String> fallbackLocales(@NotNull String locale) {
        List<String> result = new ArrayList<>(3);
        String normalized = normalize(locale);
        result.add(normalized);
        int separator = normalized.indexOf('_');
        if (separator > 0) {
            result.add(normalized.substring(0, separator));
        }
        if (!result.contains(DEFAULT_LOCALE)) {
            result.add(DEFAULT_LOCALE);
        }
        return result;
    }

    private static @NotNull String normalize(@NotNull String locale) {
        return locale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

}
