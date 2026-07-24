package dev.lucaargolo.nexo.api.unit.block;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.DataProvider;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

public abstract class BlockUnit<C extends Role> extends Unit<BlockBase, C> implements DataProvider {

    protected BlockUnit(@NotNull Nexo nexo, @NotNull BlockBase feature, @Nullable C role) {
        super(nexo, feature, role);
    }

    public abstract @Nullable WorldUnit<?> world();

    public abstract @Nullable Vector3i position();
}
