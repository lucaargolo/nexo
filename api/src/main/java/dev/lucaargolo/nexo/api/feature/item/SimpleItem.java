package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleItem extends ItemBase {

    @Nullable
    private final Model model;
    @Nullable
    private final ItemCategoryBase category;

    public SimpleItem(@NotNull Location location, @Nullable Model model, @Nullable ItemCategoryBase category) {
        super(location);
        this.model = model;
        this.category = category;
    }

    public SimpleItem(@NotNull Location location, @Nullable Model model) {
        this(location, model, null);
    }

    @Override
    public @Nullable Model model() {
        return model;
    }

    @Override
    public @Nullable ItemCategoryBase category() {
        return category;
    }

}
