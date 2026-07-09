package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Instance;
import dev.lucaargolo.nexo.api.feature.block.NexoBlock;
import dev.lucaargolo.nexo.api.feature.dimension.NexoDimension;
import dev.lucaargolo.nexo.api.feature.world.WorldInstance;
import dev.lucaargolo.nexo.api.util.Side;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

public class MinecraftWorldInstance extends WorldInstance {

    @NotNull
    private final Level level;

    public MinecraftWorldInstance(@Nullable NexoDimension feature, @NotNull Level level) {
        super(feature, level.isClientSide ? Side.CLIENT : Side.SERVER);
        this.level = level;
    }

    @Override
    @Nullable
    public Instance<NexoBlock> getBlock(@NotNull Vector3i pos) {
        BlockPos mcPos = new BlockPos(pos.x, pos.y, pos.z);
        BlockState state = level.getBlockState(mcPos);
        if (state.isAir()) return null;
        NexoBlock nexoBlock = (NexoBlock) NexoMinecraft.getInstance().getNexoFeature(state.getBlockHolder());
        if (nexoBlock == null) return null;
        return Instance.of(nexoBlock, NexoMinecraft.getInstance().getSide());
    }

    @Override
    public void setBlock(@NotNull Vector3i pos, @NotNull Instance<NexoBlock> block) {
        BlockPos mcPos = new BlockPos(pos.x, pos.y, pos.z);
        NexoBlock nexoBlock = block.get();
        if (nexoBlock == null) {
            level.setBlock(mcPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }

        Block mcBlock = NexoMinecraft.getInstance().getMinecraftFeature(nexoBlock);
        level.setBlock(mcPos, mcBlock.defaultBlockState(), Block.UPDATE_ALL);
    }

}
