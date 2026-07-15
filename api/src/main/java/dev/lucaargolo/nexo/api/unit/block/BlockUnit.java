package dev.lucaargolo.nexo.api.unit.block;

import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BlockUnit<C extends Role> extends Unit<C> {

    protected BlockUnit(@NotNull BlockBase feature, @Nullable C role) {
        super(feature, role);
    }

    @Override
    public @NotNull <R extends Role> BlockUnit<R> with(@NotNull Class<R> type) {
        return (BlockUnit<R>) super.with(type);
    }

}
