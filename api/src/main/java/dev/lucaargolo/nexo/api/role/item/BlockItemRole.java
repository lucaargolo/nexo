package dev.lucaargolo.nexo.api.role.item;

import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.role.Role;
import org.jetbrains.annotations.NotNull;

public record BlockItemRole(@NotNull BlockBase block) implements Role {

}
