package dev.lucaargolo.nexo.api.feature.component;

import dev.lucaargolo.nexo.api.feature.block.IBlock;
import org.jetbrains.annotations.NotNull;

public record BlockItemComponent(@NotNull IBlock block) implements IComponent {

}
