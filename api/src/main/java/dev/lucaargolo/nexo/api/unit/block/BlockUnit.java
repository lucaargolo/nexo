package dev.lucaargolo.nexo.api.unit.block;

import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BlockUnit<C extends Role> extends Unit<C> {

    protected BlockUnit(@NotNull BlockBase feature, @Nullable C role) {
        super(feature, role);
    }

    //TODO: This is very wrong and very temporary, units should never be created on api, only on the Nexo implementation layer.
    public static @NotNull BlockUnit<?> of(@NotNull BlockBase base) {
        return new BlockUnit<>(base, base.role()) {
            @Override
            public @Nullable <D> D getData(@NotNull DataBase<D> data) {
                return null;
            }

            @Override
            public <D> void setData(@NotNull DataBase<D> data, @Nullable D d) {

            }
        };
    }

}
