package dev.lucaargolo.nexo.api.unit.block;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BlockUnit<C extends Role> extends Unit<C> {

    protected BlockUnit(@NotNull Nexo nexo, @NotNull BlockBase feature, @Nullable C role) {
        super(nexo, feature, role);
    }

}
