package dev.lucaargolo.nexo.api.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Location {

    @NotNull private static final Map<String, Map<String, Location>> CACHE = new ConcurrentHashMap<>();

    @NotNull private final String namespace;
    @NotNull private final String path;

    private Location(@NotNull String namespace, @NotNull String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public @NotNull String namespace() {
        return namespace;
    }

    public @NotNull String path() {
        return path;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Location that = (Location) o;
        return namespace.equals(that.namespace) && path.equals(that.path);
    }

    @Override
    public int hashCode() {
        int result = namespace.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }

    @NotNull
    public static Location of(@NotNull String namespace, @NotNull String path) {
        return CACHE
                .computeIfAbsent(namespace, ns -> new ConcurrentHashMap<>())
                .computeIfAbsent(path, p -> new Location(namespace, path));
    }

}
