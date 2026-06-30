package dev.lucaargolo.nexo.feature;

import dev.lucaargolo.nexo.api.feature.item.IItem;
import dev.lucaargolo.nexo.api.feature.item.IItemCategory;
import dev.lucaargolo.nexo.api.model.Model;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public class MinecraftItem extends MinecraftFeature<Item, IItem> implements IItem {

    public MinecraftItem(Holder<Item> holder, IItem delegate) {
        super(holder, delegate);
    }

    @Override
    public @Nullable Model model() {
        return this.getDelegate().model();
    }

    @Override
    public @Nullable IItemCategory category() {
        IItem delegate = this.getDelegate();
        return delegate != null ? delegate.category() : null;
    }

}
