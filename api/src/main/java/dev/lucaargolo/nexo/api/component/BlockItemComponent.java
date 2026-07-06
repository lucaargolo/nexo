package dev.lucaargolo.nexo.api.component;

import dev.lucaargolo.nexo.api.feature.block.NexoBlock;
import org.jetbrains.annotations.NotNull;

public record BlockItemComponent(@NotNull NexoBlock block) implements Component {

}
