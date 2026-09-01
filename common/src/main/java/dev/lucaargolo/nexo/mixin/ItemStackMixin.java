package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.mixed.ItemStackMixed;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemStack.class)
public class ItemStackMixin implements ItemStackMixed {

    @Unique
    private MinecraftItemUnit<?> nexo$unit;

    @Override
    public MinecraftItemUnit<?> nexo$getUnit() {
        return this.nexo$unit;
    }

    @Override
    public void nexo$setUnit(MinecraftItemUnit<?> unit) {
        this.nexo$unit = unit;
    }
}
