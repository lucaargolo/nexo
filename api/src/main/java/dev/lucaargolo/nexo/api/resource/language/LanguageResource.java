package dev.lucaargolo.nexo.api.resource.language;

import com.google.gson.Gson;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public abstract class LanguageResource extends Resource<LanguageResource> {

    private final @NotNull String locale;
    private final @NotNull Map<String, String> entries;

    protected LanguageResource(
            @NotNull Location location,
            @NotNull String locale,
            @NotNull Map<String, String> entries
    ) {
        super(location);
        this.locale = Objects.requireNonNull(locale, "locale");
        this.entries = new LinkedHashMap<>(Objects.requireNonNull(entries, "entries"));
        this.entries.forEach((key, value) -> {
            validateKey(key);
            Objects.requireNonNull(value, "value");
        });
    }

    @Override
    public final @NotNull Type<LanguageResource> type() {
        return Type.LANGUAGE;
    }

    public final @NotNull String locale() {
        return locale;
    }

    public final @NotNull Map<String, String> entries() {
        return Collections.unmodifiableMap(entries);
    }

    public final @Nullable String entry(@NotNull String key) {
        return entries.get(key);
    }
    public final void entry(@NotNull String key, @NotNull String value) {
        Objects.requireNonNull(key, "key");
        validateKey(key);
        entries.put(key, Objects.requireNonNull(value, "value"));
    }

    private void validateKey(@NotNull String key) {
        String[] parts = key.split("\\.", -1);
        if (parts.length < 3 || !parts[1].equals(location().namespace())) {
            throw new IllegalArgumentException("Invalid Nexo translation key: " + key);
        }
        for (String part : parts) {
            if (!part.matches("[a-z0-9_-]+")) {
                throw new IllegalArgumentException("Invalid Nexo translation key: " + key);
            }
        }
    }

    public final void removeEntry(@NotNull String key) {
        entries.remove(key);
    }

    @Override
    public byte @NotNull [] data() {
        return new Gson().toJson(entries).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean resolved() {
        return true;
    }

}
