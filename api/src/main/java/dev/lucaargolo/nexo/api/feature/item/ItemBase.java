package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.ItemProvider;
import dev.lucaargolo.nexo.api.feature.ModelProvider;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ItemBase extends Feature<ItemBase> implements ModelProvider, ItemProvider {

    public ItemBase(@NotNull Location location) {
        super(location);
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
