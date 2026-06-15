package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.api.Identifier;
import dev.lucaargolo.nexo.api.feature.Block;
import net.minecraft.core.Holder;

public class MinecraftBlock extends MinecraftFeature<net.minecraft.world.level.block.Block> implements Block {

    public MinecraftBlock(Holder<net.minecraft.world.level.block.Block> holder) {
        super(holder);
    }

}
