package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

public class ItemCategory extends ItemCategoryBase {

    @NotNull
    private final Location location;

    public ItemCategory(@NotNull Location location) {
        this.location = location;
    }

    @Override
    public @NotNull Location location() {
        return location;
    }

}
