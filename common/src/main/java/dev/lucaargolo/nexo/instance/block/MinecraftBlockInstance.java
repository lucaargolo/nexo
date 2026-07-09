package dev.lucaargolo.nexo.instance.block;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.block.NexoBlock;
import dev.lucaargolo.nexo.api.feature.data.NexoData;
import dev.lucaargolo.nexo.api.instance.block.BlockInstance;
import dev.lucaargolo.nexo.instance.MinecraftInstance;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MinecraftBlockInstance extends BlockInstance implements MinecraftInstance<BlockState> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private BlockState state;

    public MinecraftBlockInstance(@NotNull NexoMinecraft nexo, @NotNull NexoBlock feature, @NotNull BlockState state) {
        super(feature);
        this.nexo = nexo;
        this.state = state;
    }

    @Override
    public @NotNull NexoMinecraft nexo() {
        return this.nexo;
    }

    public @NotNull BlockState get() {
        return this.state;
    }

    @Override
    public @Nullable <D> D getData(@NotNull NexoData<D> data) {
        return state.getValue(property);
    }

    @Override
    public <D> void setData(@NotNull NexoData<D> data, @Nullable D d) {
        this.state = state.setValue(property, d);
    }

}
