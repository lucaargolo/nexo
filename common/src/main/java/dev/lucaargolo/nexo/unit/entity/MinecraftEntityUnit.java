package dev.lucaargolo.nexo.unit.entity;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MinecraftEntityUnit<N extends NexoMinecraft<N, ?, ?, ?>, C extends Role, E extends Entity> extends EntityUnit<C> implements MinecraftUnit<E> {

    @NotNull
    protected final N nexo;
    @NotNull
    protected final E entity;

    public MinecraftEntityUnit(@NotNull N nexo, @NotNull EntityBase feature, @Nullable C role, @NotNull E entity) {
        super(nexo, feature, role);
        this.nexo = nexo;
        this.entity = entity;
    }

    @Override
    public @NotNull E get() {
        return this.entity;
    }

    @Override
    public @Nullable WorldUnit<?> world() {
        return nexo.levelToUnit(this.entity.level());
    }
}
