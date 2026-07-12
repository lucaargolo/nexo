package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.ItemProvider;
import dev.lucaargolo.nexo.api.feature.ModelProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ItemBase extends Feature<ItemBase> implements ModelProvider, ItemProvider {

    @Override
    @NotNull
    public final Class<ItemBase> type() {
        return ItemBase.class;
    }

    @Override
    @NotNull
    public ItemBase item() {
        return this;
    }

    @Nullable
    public ItemCategoryBase category() {
        return null;
    }

}
