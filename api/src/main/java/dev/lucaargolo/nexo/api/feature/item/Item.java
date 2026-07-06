package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Item extends BaseItem {

    @NotNull
    private final Location location;
    @Nullable
    private final Model model;
    @Nullable
    private final BaseItemCategory category;

    public Item(@NotNull Location location, @Nullable Model model, @Nullable BaseItemCategory category) {
        this.location = location;
        this.model = model;
        this.category = category;
    }

    public Item(@NotNull Location location, @Nullable Model model) {
        this(location, model, null);
    }

    @Override
    public @NotNull Location location() {
        return location;
    }

    @Override
    public @Nullable Model model() {
        return model;
    }

    @Override
    public @Nullable BaseItemCategory category() {
        return category;
    }

}
