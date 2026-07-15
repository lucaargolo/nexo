package dev.lucaargolo.nexo.api.role;

import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import org.jetbrains.annotations.NotNull;

public record BlockItemRole(@NotNull BlockBase block) implements Role {

}
