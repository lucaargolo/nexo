package dev.lucaargolo.nexo.api.role.entity;

import dev.lucaargolo.nexo.api.role.Role;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record PlayerRole(@NotNull UUID uuid, @NotNull String name) implements Role {

}
