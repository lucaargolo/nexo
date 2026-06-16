package dev.lucaargolo.nexo;

import java.nio.file.Path;

public record NexoMod(
        String modId,
        String name,
        String description,
        String version,
        String[] authors,
        String className,
        Path sourceJar
) {

    public NexoMod(String modId, String name, String description, String version, String[] authors, String className, Path sourceJar) {
        this.modId = modId;
        this.name = name.isEmpty() ? modId : name;
        this.description = description;
        this.version = version;
        this.authors = authors;
        this.className = className;
        this.sourceJar = sourceJar;
    }
}
