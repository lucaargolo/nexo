package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.mixed.EntityMixed;
import dev.lucaargolo.nexo.unit.entity.MinecraftEntityUnit;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityMixed {

    @Unique
    private MinecraftEntityUnit<?, ?, ?> nexo$unit;

    @Override
    public MinecraftEntityUnit<?, ?, ?> nexo$getUnit() {
        return this.nexo$unit;
    }

    @Override
    public void nexo$setUnit(MinecraftEntityUnit<?, ?, ?> unit) {
        this.nexo$unit = unit;
    }
}
