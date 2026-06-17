package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.api.feature.IBlock;
import dev.lucaargolo.nexo.api.model.Model;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class MinecraftBlock extends MinecraftFeature<Block, IBlock> implements IBlock {

    public MinecraftBlock(Holder<Block> holder, IBlock delegate) {
        super(holder, delegate);
    }

    @Override
    public @Nullable Model model() {
        return this.getDelegate().model();
    }

}
