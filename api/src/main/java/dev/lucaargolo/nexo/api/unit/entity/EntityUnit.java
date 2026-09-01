package dev.lucaargolo.nexo.api.unit.entity;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.packet.PacketReceiver;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.feature.DataProvider;
import dev.lucaargolo.nexo.api.feature.SideProvider;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class EntityUnit<C extends Role> extends Unit<EntityBase, C> implements SideProvider, DataProvider, PacketReceiver {

    protected EntityUnit(@NotNull Nexo nexo, @NotNull EntityBase feature, @Nullable C role) {
        super(nexo, feature, role);
    }

    public abstract @Nullable WorldUnit<?> world();

    public abstract boolean openScreen(@NotNull ScreenBase screen);

}
