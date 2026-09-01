package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.mixed.UnitCacheMixed;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({BlockEntity.class, BlockState.class, CreativeModeTab.class, Entity.class, ItemStack.class, Level.class})
public abstract class UnitCacheMixin implements UnitCacheMixed {

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
