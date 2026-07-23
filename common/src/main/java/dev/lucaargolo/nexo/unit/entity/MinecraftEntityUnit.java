package dev.lucaargolo.nexo.unit.entity;

import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MinecraftEntityUnit<R extends NexoRegistryHandler<?>, C extends Role, E extends Entity> extends EntityUnit<C> implements MinecraftUnit<E> {

    @NotNull
    protected final R helper;
    @NotNull
    protected final E entity;

    public MinecraftEntityUnit(@NotNull R helper, @NotNull EntityBase feature, @Nullable C role, @NotNull E entity) {
        super(helper.nexo(), feature, role);
        this.helper = helper;
        this.entity = entity;
    }

    @Override
    public @NotNull E get() {
        return this.entity;
    }
}
