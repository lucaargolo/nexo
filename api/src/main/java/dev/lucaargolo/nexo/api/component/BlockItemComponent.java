package dev.lucaargolo.nexo.api.component;

import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import org.jetbrains.annotations.NotNull;

public record BlockItemComponent(@NotNull BlockBase block) implements Component {

}
