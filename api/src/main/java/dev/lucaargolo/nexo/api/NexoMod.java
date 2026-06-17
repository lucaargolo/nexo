package dev.lucaargolo.nexo.api;

import java.nio.file.Path;

public record NexoMod(
        String value,
        String name,
        String description,
        String version,
        String[] authors,
        Path path
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
