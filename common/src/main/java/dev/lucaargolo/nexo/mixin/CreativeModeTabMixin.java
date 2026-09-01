package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.mixed.CreativeModeTabMixed;
import dev.lucaargolo.nexo.unit.item.MinecraftItemCategoryUnit;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CreativeModeTab.class)
public class CreativeModeTabMixin implements CreativeModeTabMixed {

    @Unique
    private MinecraftItemCategoryUnit<?, ?> nexo$unit;

    @Override
    public MinecraftItemCategoryUnit<?, ?> nexo$getUnit() {
        return this.nexo$unit;
    }

    @Override
    public void nexo$setUnit(MinecraftItemCategoryUnit<?, ?> unit) {
        this.nexo$unit = unit;
    }
}
