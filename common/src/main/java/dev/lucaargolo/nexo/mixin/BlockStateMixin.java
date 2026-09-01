package dev.lucaargolo.nexo.mixin;

import dev.lucaargolo.nexo.mixed.BlockStateMixed;
import dev.lucaargolo.nexo.unit.block.MinecraftBlockUnit;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockState.class)
public class BlockStateMixin implements BlockStateMixed {

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
