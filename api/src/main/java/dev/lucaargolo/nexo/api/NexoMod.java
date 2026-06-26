package dev.lucaargolo.nexo.api;

import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;

public record NexoMod(
        @NotNull String value,
        @NotNull String name,
        @NotNull String description,
        @NotNull String version,
        @NotNull String[] authors,
        @NotNull Path path
) {

    public NexoMod(String value, String name, String description, String version, String[] authors, Path path) {
        this.value = value;
        this.name = name.isEmpty() ? value : name;
        this.description = description;
        this.version = version;
        this.authors = authors;
        this.path = path;
    }
}
