package dev.lucaargolo.nexo.api.unit.entity;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.DataProvider;
import dev.lucaargolo.nexo.api.feature.SideProvider;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.packet.PacketReceiver;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class EntityUnit extends Unit<EntityBase> implements SideProvider, DataProvider<EntityUnit>, PacketReceiver {

    protected EntityUnit(@NotNull Nexo nexo, @NotNull EntityBase feature, @Nullable Role role) {
        super(nexo, feature, role);
    }

    public abstract @Nullable WorldUnit world();

}
