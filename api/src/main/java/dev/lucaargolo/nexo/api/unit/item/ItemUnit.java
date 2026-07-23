package dev.lucaargolo.nexo.api.unit.item;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ItemUnit<C extends Role> extends Unit<C> {

    protected ItemUnit(@NotNull Nexo nexo, @NotNull ItemBase feature, @Nullable C role) {
        super(nexo, feature, role);
    }

}
