package dev.lucaargolo.nexo.unit.world;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.world.NexoWorld;
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

public abstract class MinecraftWorldUnit extends WorldUnit implements MinecraftUnit<Level> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    protected final Level level;

    public MinecraftWorldUnit(@NotNull NexoMinecraft nexo, @NotNull NexoWorld feature, @NotNull Level level) {
        super(feature);
        this.nexo = nexo;
        this.level = level;
    }

    @Override
    public @NotNull NexoMinecraft nexo() {
        return this.nexo;
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
    @Nullable
    public BlockUnit getBlock(@NotNull Vector3i pos) {
        BlockPos mcPos = new BlockPos(pos.x, pos.y, pos.z);
        BlockState state = level.getBlockState(mcPos);
        if (state.isAir()) return null;
        return this.nexo.block(state);
    }

    @Override
    public void setBlock(@NotNull Vector3i pos, @NotNull BlockUnit block) {
        BlockPos mcPos = new BlockPos(pos.x, pos.y, pos.z);
        level.setBlockAndUpdate(mcPos, ((MinecraftBlockUnit) block).get());
    }

}
