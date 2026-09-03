package dev.lucaargolo.nexo.api.unit.block;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.DataProvider;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

public abstract class BlockUnit extends Unit<BlockBase> implements DataProvider<BlockUnit> {

    protected BlockUnit(@NotNull Nexo nexo, @NotNull BlockBase feature, @Nullable Role role) {
        super(nexo, feature, role);
    }

    public abstract @Nullable WorldUnit world();

    public abstract @Nullable Vector3i position();

    public abstract void drop(@NotNull ItemUnit item);
}
