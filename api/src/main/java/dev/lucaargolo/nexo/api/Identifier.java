package dev.lucaargolo.nexo.api;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Identifier {

    private static final Map<String, Map<String, Identifier>> CACHE = new ConcurrentHashMap<>();

    @NotNull private final String namespace;
    @NotNull private final String path;

    private Identifier(@NotNull String namespace, @NotNull String path) {
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
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Identifier that = (Identifier) o;
        return namespace.equals(that.namespace) && path.equals(that.path);
    }

    @Override
    public int hashCode() {
        int result = namespace.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }

    @NotNull
    public static Identifier of(String namespace, String path) {
        return CACHE
                .computeIfAbsent(namespace, ns -> new ConcurrentHashMap<>())
                .computeIfAbsent(path, p -> new Identifier(namespace, path));
    }

}
