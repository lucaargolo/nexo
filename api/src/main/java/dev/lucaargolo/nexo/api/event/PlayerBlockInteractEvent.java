package dev.lucaargolo.nexo.api.event;

import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import org.jetbrains.annotations.NotNull;

public record PlayerBlockInteractEvent(@NotNull BlockUnit block, @NotNull EntityUnit player) implements Event<BlockUnit> {

    @Override
    public @NotNull BlockUnit value() {
        return this.block;
    }

    @Override
    public boolean cancelable() {
        return true;
    }

}
