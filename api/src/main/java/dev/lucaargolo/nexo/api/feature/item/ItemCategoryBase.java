package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

public abstract class ItemCategoryBase extends Feature<ItemCategoryBase> {

    public ItemCategoryBase(@NotNull Location location) {
        super(location);
    }

}
