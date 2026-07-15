package dev.lucaargolo.nexo.api.role;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record PlayerRole(@NotNull UUID uuid, @NotNull String name) implements Role {

}
