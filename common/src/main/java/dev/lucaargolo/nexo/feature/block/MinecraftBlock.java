package dev.lucaargolo.nexo.feature.block;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Interaction;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeature;
import dev.lucaargolo.nexo.util.NexoHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import java.util.List;

public class MinecraftBlock extends BlockBase implements MinecraftFeature<BlockBase, Block> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final NexoHolder<Block, Block> holder;
    @Nullable
    private final BlockBase delegate;

    protected MinecraftBlock(@NotNull NexoMinecraft nexo, @NotNull NexoHolder<Block, Block> holder, @Nullable BlockBase delegate) {
        super(holder.location());
        this.nexo = nexo;
        this.delegate = delegate;
        this.holder = holder;
    }

    public MinecraftBlock(@NotNull NexoMinecraft nexo, @NotNull Holder<Block> holder) {
        this(nexo, new NexoHolder<>(nexo, holder, Block.class), null);
    }

    @Override
    public @NotNull NexoMinecraft nexo() {
        return this.nexo;
    }

    @Override
    public @NotNull NexoHolder<Block, Block> holder() {
        return this.holder;
    }

    @Override
    public @Nullable BlockBase delegate() {
        return this.delegate;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    @Override
    public @Nullable Model model() {
        //TODO: This
        return this.delegate != null ? this.delegate.model() : null;
    }

    @Override
    public @Nullable ItemBase item() {
        if(this.delegate != null) {
            return this.delegate.item();
        }else{
            Item item = this.holder().get().asItem();
            if(item != Items.AIR) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
                return this.nexo.getFeature(ItemBase.class, NexoMinecraft.id(itemId));
            }else{
                return null;
            }
        }
    }

    public static MinecraftBlock register(NexoRegistryHandler<?> helper, ResourceLocation id, BlockBase block) {
        NexoHolder<Block, Block> holder = helper.registerBuiltinFeature(BuiltInRegistries.BLOCK, id, () -> new Block(BlockBehaviour.Properties.of()) {
            @Override
            protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull Player pPlayer, @NotNull BlockHitResult pHitResult) {
                WorldUnit world = helper.nexo().world(pLevel);
                Interaction interaction = block.onInteract(world, new Vector3i(pPos.getX(), pPos.getY(), pPos.getZ()));
                return switch (interaction) {
                    case PASS -> InteractionResult.PASS;
                    case FAIL -> InteractionResult.FAIL;
                    case SUCCESS -> InteractionResult.SUCCESS;
                };
            }
        });
        return new MinecraftBlock(helper.nexo(), holder, block);
    }

}
