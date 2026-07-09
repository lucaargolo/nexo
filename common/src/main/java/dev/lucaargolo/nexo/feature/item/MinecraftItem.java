package dev.lucaargolo.nexo.feature.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.component.BlockItemComponent;
import dev.lucaargolo.nexo.api.feature.item.NexoItem;
import dev.lucaargolo.nexo.api.feature.item.NexoItemCategory;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeature;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MinecraftItem extends NexoItem implements MinecraftFeature<NexoItem, Item> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final Location location;
    @NotNull
    private final Holder<Item> holder;
    @Nullable
    private final NexoItem delegate;

    public MinecraftItem(@NotNull NexoMinecraft nexo, @NotNull Holder<Item> holder, @Nullable NexoItem delegate) {
        this.nexo = nexo;
        this.delegate = delegate;
        this.holder = holder;
        this.location = NexoMinecraft.id(holder.unwrapKey().orElseThrow().location());
    }

    public MinecraftItem(@NotNull NexoMinecraft nexo, Holder<Item> holder) {
        this(nexo, holder, null);
    }

    @Override
    public @NotNull NexoMinecraft nexo() {
        return this.nexo;
    }

    @Override
    public @NotNull Holder<Item> holder() {
        return this.holder;
    }

    @Override
    public @Nullable NexoItem delegate() {
        return this.delegate;
    }

    @Override
    public @NotNull Location location() {
        return this.location;
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
    public @Nullable NexoItemCategory category() {
        //TODO: This
        return this.delegate != null ? this.delegate.category() : null;
    }

    public static MinecraftItem register(NexoRegistryHandler<?> helper, ResourceLocation id, NexoItem item) {
        Holder<Item> holder = helper.registerBuiltinFeature(BuiltInRegistries.ITEM, id, () -> {
            if (item.hasComponent(BlockItemComponent.class)) {
                BlockItemComponent component = item.getComponent(BlockItemComponent.class);
                assert component != null;
                Block block = (Block) ((MinecraftFeature<?, ?>) component.block()).holder().value();
                return new BlockItem(block, new Item.Properties());
            } else {
                return new Item(new Item.Properties());
            }
        });
        return new MinecraftItem(helper.nexo(), holder, item);
    }

}
