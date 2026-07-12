package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.Feature;
import org.jetbrains.annotations.NotNull;

public abstract class ItemCategoryBase extends Feature<ItemCategoryBase> {

    @Override
    @NotNull
    public final Class<ItemCategoryBase> type() {
        return ItemCategoryBase.class;
    }

}
