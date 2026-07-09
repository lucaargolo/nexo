package dev.lucaargolo.nexo.instance.world;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.data.NexoData;
import dev.lucaargolo.nexo.api.feature.world.NexoWorld;
import dev.lucaargolo.nexo.api.instance.block.BlockInstance;
import dev.lucaargolo.nexo.api.instance.world.WorldInstance;
import dev.lucaargolo.nexo.api.util.Side;
import dev.lucaargolo.nexo.instance.MinecraftInstance;
import dev.lucaargolo.nexo.instance.block.MinecraftBlockInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

public class MinecraftWorldInstance extends WorldInstance implements MinecraftInstance<Level> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final Level level;

    public MinecraftWorldInstance(@NotNull NexoMinecraft nexo, @NotNull NexoWorld feature, @NotNull Level level) {
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
    public <D> D getData(@NotNull NexoData<D> data) {
        return null;
    }

    @Override
    public <D> void setData(@NotNull NexoData<D> data, @Nullable D d) {

    }

    @Override
    @Nullable
    public BlockInstance getBlock(@NotNull Vector3i pos) {
        BlockPos mcPos = new BlockPos(pos.x, pos.y, pos.z);
        BlockState state = level.getBlockState(mcPos);
        if (state.isAir()) return null;
        return this.nexo.block(state);
    }

    @Override
    public void setBlock(@NotNull Vector3i pos, @NotNull BlockInstance block) {
        BlockPos mcPos = new BlockPos(pos.x, pos.y, pos.z);
        level.setBlockAndUpdate(mcPos, ((MinecraftBlockInstance) block).get());
    }

}
