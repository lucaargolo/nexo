package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.mixed.UnitCacheMixed;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Screen.class)
public abstract class ClientUnitCacheMixin implements UnitCacheMixed {

    @Unique
    private MinecraftUnit<?> nexo$unit;

    @Override
    public MinecraftUnit<?> nexo$getUnit() {
        return this.nexo$unit;
    }

    @Override
    public void nexo$setUnit(MinecraftUnit<?> unit) {
        this.nexo$unit = unit;
    }
}
