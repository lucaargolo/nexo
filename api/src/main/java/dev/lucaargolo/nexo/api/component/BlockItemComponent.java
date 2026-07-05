package dev.lucaargolo.nexo.api.component;

import dev.lucaargolo.nexo.api.feature.block.BaseBlock;
import org.jetbrains.annotations.NotNull;

public record BlockItemComponent(@NotNull BaseBlock block) implements Component {

}
