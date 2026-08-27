package dev.lucaargolo.nexo.resource.language;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.resource.language.LanguageResource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MinecraftLanguageResource extends LanguageResource {

    private static final @NotNull Map<Location, LanguageResource> RESOURCE_MAP = new ConcurrentHashMap<>();

    private MinecraftLanguageResource(
            @NotNull Location location,
            @NotNull String locale,
            @NotNull Map<String, String> entries
    ) {
        super(location, locale, entries);
    }

    public static @Nullable LanguageResource lookup(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Location location) {
        LanguageResource registered = RESOURCE_MAP.get(location);
        if (registered != null) {
            return registered;
        }
        byte[] data = nexo.loadResource(location);
        if (data == null) {
            return null;
        }
        try {
            return parse(location, data);
        } catch (RuntimeException exception) {
            NexoMinecraft.LOGGER.warn("Could not parse language resource {}", location, exception);
            return null;
        }
    }

    public static @NotNull LanguageResource register(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull Location location
    ) {
        LanguageResource resource = lookup(nexo, location);
        if (resource == null) {
            throw new IllegalArgumentException("Could not load language resource " + location);
        }
        return register(nexo, resource);
    }

    public static @NotNull LanguageResource register(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull Location location,
            byte @NotNull [] data
    ) {
        return register(nexo, parse(location, data));
    }

    private static @NotNull LanguageResource register(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull LanguageResource resource
    ) {
        RESOURCE_MAP.put(resource.location(), resource);
        nexo.getLanguageHandler().register(resource);
        return resource;
    }

    public static @NotNull LanguageResource parse(@NotNull Location location, byte @NotNull [] data) {
        String locale = locale(location);
        JsonObject object;
        try {
            object = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid language resource " + location, exception);
        }

        Map<String, String> entries = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Language entry must be a string: " + entry.getKey());
            }
            entries.put(entry.getKey(), entry.getValue().getAsString());
        }
        return new MinecraftLanguageResource(location, locale, entries);
    }

    private static @NotNull String locale(@NotNull Location location) {
        String path = location.path();
        int slash = path.lastIndexOf('/');
        String file = slash < 0 ? path : path.substring(slash + 1);
        if (!file.endsWith(".json")) {
            throw new IllegalArgumentException("Language resource must be a JSON file: " + location);
        }
        String locale = file.substring(0, file.length() - ".json".length());
        if (!locale.matches("[a-z]{2,3}(?:_[a-z]{2})?")) {
            throw new IllegalArgumentException("Invalid language locale in " + location);
        }
        return locale;
    }

}
