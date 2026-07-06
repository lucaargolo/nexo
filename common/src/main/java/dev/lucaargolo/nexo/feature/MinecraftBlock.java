package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.block.NexoBlock;
import dev.lucaargolo.nexo.api.feature.item.NexoItem;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MinecraftBlock extends NexoBlock {

    @NotNull
    private final Location location;
    @NotNull
    private final Holder<Block> holder;
    @Nullable
    private final NexoBlock delegate;

    public MinecraftBlock(Holder<Block> holder, NexoBlock delegate) {
        this.delegate = delegate;
        this.holder = holder;
        this.location = NexoMinecraft.id(holder.unwrapKey().orElseThrow().location());
    }

    public MinecraftBlock(Holder<Block> holder) {
        this(holder, null);
    }

    public @NotNull Holder<Block> getHolder() {
        return holder;
    }

    @Nullable
    public NexoBlock getDelegate() {
        return delegate;
    }

    @Override
    public @NotNull Location location() {
        return location;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    @Override
    public @Nullable Model model() {
        //TODO: This
        return delegate != null ? delegate.model() : null;
    }

    @Override
    public @Nullable NexoItem item() {
        if(delegate != null) {
            return delegate.item();
        }else{
            Item item = this.getHolder().value().asItem();
            if(item != Items.AIR) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
                return NexoMinecraft.getInstance().getFeature(NexoItem.class, NexoMinecraft.id(itemId));
            }else{
                return null;
            }
        }
    }

    public static MinecraftBlock register(ResourceLocation id, NexoBlock block) {
        Holder<Block> holder = NexoMinecraft.getHelper().registerFeature(BuiltInRegistries.BLOCK, id, () ->
                new Block(BlockBehaviour.Properties.of()));
        return new MinecraftBlock(holder, block);
    }

}
