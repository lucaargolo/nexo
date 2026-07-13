package dev.lucaargolo.nexo.feature.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.component.BlockItemComponent;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.item.ItemCategoryBase;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeature;
import dev.lucaargolo.nexo.util.NexoHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MinecraftItem extends ItemBase implements MinecraftFeature<ItemBase, Item> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final NexoHolder<Item, Item> holder;
    @Nullable
    private final ItemBase delegate;

    public MinecraftItem(@NotNull NexoMinecraft nexo, @NotNull NexoHolder<Item, Item> holder, @Nullable ItemBase delegate) {
        super(holder.location());
        this.nexo = nexo;
        this.delegate = delegate;
        this.holder = holder;
    }

    public MinecraftItem(@NotNull NexoMinecraft nexo, Holder<Item> holder) {
        this(nexo, new NexoHolder<>(nexo, holder, Item.class), null);
    }

    @Override
    public @NotNull NexoMinecraft nexo() {
        return this.nexo;
    }

    @Override
    public @NotNull NexoHolder<Item, Item> holder() {
        return this.holder;
    }

    @Override
    public @Nullable ItemBase delegate() {
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
    public @Nullable ItemCategoryBase category() {
        //TODO: This
        return this.delegate != null ? this.delegate.category() : null;
    }

    public static MinecraftItem register(NexoRegistryHandler<?> helper, ResourceLocation id, ItemBase item) {
        NexoHolder<Item, Item> holder = helper.registerBuiltinFeature(BuiltInRegistries.ITEM, id, () -> {
            if (item.hasComponent(BlockItemComponent.class)) {
                BlockItemComponent component = item.getComponent(BlockItemComponent.class);
                assert component != null;
                Block block = (Block) ((MinecraftFeature<?, ?>) component.block()).holder().get();
                return new BlockItem(block, new Item.Properties());
            } else {
                return new Item(new Item.Properties());
            }
        });
        return new MinecraftItem(helper.nexo(), holder, item);
    }

}
