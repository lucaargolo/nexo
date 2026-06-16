package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.api.feature.IBlock;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;

public class MinecraftBlock extends MinecraftFeature<Block> implements IBlock {

    public MinecraftBlock(Holder<Block> holder) {
        super(holder);
    }

}
