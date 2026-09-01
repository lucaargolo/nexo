package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.mixed.LevelMixed;
import dev.lucaargolo.nexo.unit.world.MinecraftWorldUnit;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Level.class)
public abstract class LevelMixin implements LevelMixed {

    @Unique
    private MinecraftWorldUnit<?> nexo$unit;

    @Override
    public MinecraftWorldUnit<?> nexo$getUnit() {
        return this.nexo$unit;
    }

    @Override
    public void nexo$setUnit(MinecraftWorldUnit<?> unit) {
        this.nexo$unit = unit;
    }
}
