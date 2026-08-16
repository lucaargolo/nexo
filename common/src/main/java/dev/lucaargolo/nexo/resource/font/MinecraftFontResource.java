package dev.lucaargolo.nexo.resource.font;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.font.Font;
import dev.lucaargolo.nexo.api.resource.font.FontResource;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.render.MinecraftFont;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class MinecraftFontResource extends FontResource {

    private static final Map<Location, FontResource> RESOURCE_MAP = new ConcurrentHashMap<>();

    private final boolean resolved;

    private MinecraftFontResource(Location location, boolean resolved, Supplier<Font> supplier) {
        super(location, supplier);
        this.resolved = resolved;
    }

    @Override
    public boolean resolved() {
        return font != null || resolved;
    }

    public static FontResource lookup(NexoMinecraft nexo, Location location) {
        FontResource registered = RESOURCE_MAP.get(location);
        if (registered != null) {
            return registered;
        }

        Font loaded = Font.load(nexo, location);
        return RESOURCE_MAP.computeIfAbsent(location, key -> new MinecraftFontResource(
                location,
                loaded != null,
                loaded != null ? () -> loaded : () -> Font.load(nexo, location)
        ));
    }

    @NotNull
    public static FontResource register(@NotNull NexoMinecraft nexo, @NotNull Location location) {
        Font loaded = Font.load(nexo, location);
        return register(location, loaded);
    }

    @NotNull
    public static FontResource register(@NotNull NexoMinecraft nexo, @NotNull Location location, byte @NotNull [] data) {
        Font loaded = Font.load(nexo, location, data);
        return register(location, loaded);
    }

    private static FontResource register(@NotNull Location location, Font loaded) {
        if (loaded == null) {
            throw new IllegalArgumentException("Could not load font " + location);
        }

        FontResource resource = new MinecraftFontResource(location, true, () -> loaded);
        RESOURCE_MAP.put(location, resource);
        MinecraftFont.register(location, loaded);
        return resource;
    }

}
