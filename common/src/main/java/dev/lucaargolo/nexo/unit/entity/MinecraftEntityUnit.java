package dev.lucaargolo.nexo.unit.entity;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public abstract class MinecraftEntityUnit extends EntityUnit<EntityBase> implements MinecraftUnit<Entity> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    protected final Entity entity;

    public MinecraftEntityUnit(@NotNull NexoMinecraft nexo, @NotNull EntityBase feature, @NotNull Entity entity) {
        super(feature);
        this.nexo = nexo;
        this.entity = entity;
    }

    @Override
    public @NotNull NexoMinecraft nexo() {
        return this.nexo;
    }

    @Override
    public @NotNull Entity get() {
        return this.entity;
    }
}
