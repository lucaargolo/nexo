package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.block.IBlock;
import dev.lucaargolo.nexo.api.model.Model;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.Nullable;

public class MinecraftBlock extends MinecraftFeature<Block, IBlock> implements IBlock {

    public MinecraftBlock(Holder<Block> holder, IBlock delegate) {
        super(holder, delegate);
    }

    public MinecraftBlock(Holder<Block> holder) {
        super(holder, null);
    }

    @Override
    public @Nullable Model model() {
        //TODO: This
        return this.getDelegate().model();
    }

    public static MinecraftBlock register(ResourceLocation id, IBlock block) {
        Holder<Block> holder = NexoMinecraft.getHelper().registerFeature(BuiltInRegistries.BLOCK, id, () -> {
            return new Block(BlockBehaviour.Properties.of());
        });
        return new MinecraftBlock(holder, block);
    }

}
