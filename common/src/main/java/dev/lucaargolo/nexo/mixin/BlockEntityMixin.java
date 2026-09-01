package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.mixed.BlockEntityMixed;
import dev.lucaargolo.nexo.unit.block.MinecraftBlockUnit;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements BlockEntityMixed {

    @Unique
    private MinecraftBlockUnit<?, ?> nexo$unit;

    @Override
    public MinecraftBlockUnit<?, ?> nexo$getUnit() {
        return this.nexo$unit;
    }

    @Override
    public void nexo$setUnit(MinecraftBlockUnit<?, ?> unit) {
        this.nexo$unit = unit;
    }
}
