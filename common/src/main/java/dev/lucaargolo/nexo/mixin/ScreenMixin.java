package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.mixed.ScreenMixed;
import dev.lucaargolo.nexo.unit.screen.MinecraftScreenUnit;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Screen.class)
public abstract class ScreenMixin implements ScreenMixed {

    @Unique
    private MinecraftScreenUnit<?> nexo$unit;

    @Override
    public MinecraftScreenUnit<?> nexo$getUnit() {
        return this.nexo$unit;
    }

    @Override
    public void nexo$setUnit(MinecraftScreenUnit<?> unit) {
        this.nexo$unit = unit;
    }
}
