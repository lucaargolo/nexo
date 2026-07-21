package dev.lucaargolo.nexo.api.render.model.loader;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.StringTokenizer;

final class ModelResources {

    private ModelResources() {
    }

    static @NotNull Location resolve(@NotNull Location base, @NotNull String reference) {
        String value = reference.replace('\\', '/');
        int suffix = value.indexOf('?');
        if (suffix >= 0) value = value.substring(0, suffix);
        suffix = value.indexOf('#');
        if (suffix >= 0) value = value.substring(0, suffix);
        value = URLDecoder.decode(value, StandardCharsets.UTF_8);

        int colon = value.indexOf(':');
        int firstSlash = value.indexOf('/');
        if (colon > 0 && (firstSlash < 0 || colon < firstSlash)) {
            return Location.of(value.substring(0, colon), normalize(value.substring(colon + 1)));
        }
        if (value.startsWith("/")) {
            return Location.of(base.namespace(), normalize(value.substring(1)));
        }
        int slash = base.path().lastIndexOf('/');
        String parent = slash < 0 ? "" : base.path().substring(0, slash + 1);
        return Location.of(base.namespace(), normalize(parent + value));
    }

    static @NotNull String normalize(@NotNull String path) {
        Deque<String> segments = new ArrayDeque<>();
        StringTokenizer tokenizer = new StringTokenizer(path, "/");
        while (tokenizer.hasMoreTokens()) {
            String segment = tokenizer.nextToken();
            if (segment.isEmpty() || segment.equals(".")) continue;
            if (segment.equals("..")) {
                if (segments.isEmpty()) throw new IllegalArgumentException("Resource path escapes its namespace: " + path);
                segments.removeLast();
            } else {
                segments.addLast(segment);
            }
        }
        return String.join("/", segments);
    }

}
