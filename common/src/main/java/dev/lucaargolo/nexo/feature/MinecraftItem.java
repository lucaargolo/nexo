package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.component.BlockItemComponent;
import dev.lucaargolo.nexo.api.feature.block.IBlock;
import dev.lucaargolo.nexo.api.feature.item.IItem;
import dev.lucaargolo.nexo.api.feature.item.IItemCategory;
import dev.lucaargolo.nexo.api.model.Model;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.Nullable;

public class MinecraftItem extends MinecraftFeature<Item, IItem> implements IItem {

    public MinecraftItem(Holder<Item> holder, IItem delegate) {
        super(holder, delegate);
    }

    public MinecraftItem(Holder<Item> holder) {
        super(holder, null);
    }

    @Override
    public @Nullable Model model() {
        //TODO: This
        return this.getDelegate().model();
    }

    @Override
    public @Nullable IItemCategory category() {
        IItem delegate = this.getDelegate();
        return delegate != null ? delegate.category() : null;
    }

    public static MinecraftItem register(ResourceLocation id, IItem item) {
        Holder.Reference<Item> holder = NexoMinecraft.getHelper().registerFeature(BuiltInRegistries.ITEM, id, () -> {
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
