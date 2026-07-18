package dev.lucaargolo.nexo.api.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class Location {

    @NotNull
    private static final Map<String, Map<String, Location>> CACHE = new ConcurrentHashMap<>();

    @NotNull
    private final String namespace;
    @NotNull
    private final String path;

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

    public @NotNull Location withNamespace(@NotNull String namespace) {
        return Location.of(namespace, path);
    }

    public @NotNull Location withNamespace(@NotNull Function<Location, String> function) {
        return Location.of(function.apply(this), path);
    }

    public @NotNull Location withPath(@NotNull String path) {
        return Location.of(namespace, path);
    }

    public @NotNull Location withPath(@NotNull Function<Location, String> function) {
        return Location.of(namespace, function.apply(this));
    }

    public @NotNull Location withNamespacePrefix(@NotNull String prefix) {
        return Location.of(prefix + namespace, path);
    }

    public @NotNull Location withPathPrefix(@NotNull String prefix) {
        return Location.of(namespace, prefix + path);
    }

    public @NotNull Location withNamespaceSuffix(@NotNull String suffix) {
        return Location.of(namespace + suffix, path);
    }

    public @NotNull Location withPathSuffix(@NotNull String suffix) {
        return Location.of(namespace, path + suffix);
    }

    public @NotNull Location withoutExtension() {
        return withPath(l -> {
            String path = l.path();
            int dot = path.lastIndexOf('.');
            if (dot > -1) path = path.substring(0, dot);
            return path;
        });
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

    public static @NotNull Location of(@NotNull String namespace, @NotNull String path) {
        return CACHE
                .computeIfAbsent(namespace, ns -> new ConcurrentHashMap<>())
                .computeIfAbsent(path, p -> new Location(namespace, path));
    }

    @Override
    public @NotNull String toString() {
        return this.namespace + ":" + this.path;
    }

}
