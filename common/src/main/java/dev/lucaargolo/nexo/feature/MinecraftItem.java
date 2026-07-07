package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.component.BlockItemComponent;
import dev.lucaargolo.nexo.api.feature.item.NexoItem;
import dev.lucaargolo.nexo.api.feature.item.NexoItemCategory;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MinecraftItem extends NexoItem implements MinecraftFeature<NexoItem, Item> {

    @NotNull
    private final Location location;
    @NotNull
    private final Holder<Item> holder;
    @Nullable
    private final NexoItem delegate;

    public MinecraftItem(Holder<Item> holder, NexoItem delegate) {
        this.delegate = delegate;
        this.holder = holder;
        this.location = NexoMinecraft.id(holder.unwrapKey().orElseThrow().location());
    }

    public MinecraftItem(Holder<Item> holder) {
        this(holder, null);
    }

    @Override
    public @NotNull Holder<Item> holder() {
        return holder;
    }

    @Override
    public @Nullable NexoItem delegate() {
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
    public @Nullable NexoItemCategory category() {
        return delegate != null ? delegate.category() : null;
    }

    public static MinecraftItem register(ResourceLocation id, NexoItem item) {
        Holder<Item> holder = NexoMinecraft.getHelper().registerBuiltinFeature(BuiltInRegistries.ITEM, id, () -> {
            if (item.hasComponent(BlockItemComponent.class)) {
                BlockItemComponent component = item.getComponent(BlockItemComponent.class);
                assert component != null;
                return new BlockItem(NexoMinecraft.getInstance().getMinecraftFeature(component.block()), new Item.Properties());
            } else {
                return new Item(new Item.Properties());
            }
        });
        return new MinecraftItem(holder, item);
    }

}
