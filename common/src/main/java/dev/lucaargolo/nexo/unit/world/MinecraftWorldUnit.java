package dev.lucaargolo.nexo.unit.world;

import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.world.WorldBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.unit.MinecraftUnit;
import dev.lucaargolo.nexo.unit.block.MinecraftBlockUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

public abstract class MinecraftWorldUnit<R extends NexoRegistryHandler<?>> extends WorldUnit<Role> implements MinecraftUnit<Level> {

    @NotNull
    protected final R helper;
    @NotNull
    protected final Level level;

    public MinecraftWorldUnit(@NotNull R helper, @NotNull WorldBase feature, @Nullable Role role, @NotNull Level level) {
        super(feature, role);
        this.helper = helper;
        this.level = level;
    }

    @Override
    public @NotNull Level get() {
        return this.level;
    }

    @Override
    public @NotNull Side side() {
        return this.level.isClientSide ? Side.CLIENT : Side.SERVER;
    }

    @Override
    public @Nullable BlockUnit<?> getBlock(@NotNull Vector3i pos) {
        BlockPos mcPos = new BlockPos(pos.x, pos.y, pos.z);
        BlockState state = level.getBlockState(mcPos);
        if (state.isAir()) return null;
        return this.helper.nexo().stateToUnit(state);
    }

    @Override
    public void setBlock(@NotNull Vector3i pos, @NotNull BlockUnit<?> block) {
        BlockPos mcPos = new BlockPos(pos.x, pos.y, pos.z);
        level.setBlockAndUpdate(mcPos, ((MinecraftBlockUnit) block).get());
    }

}
