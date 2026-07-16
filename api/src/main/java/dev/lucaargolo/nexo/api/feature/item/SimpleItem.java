package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.render.model.ModelRenderer;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleItem extends ItemBase {

    private final @Nullable ItemCategoryBase category;
    private final @Nullable StaticRenderer<Graphics3D, ItemUnit<?>> renderer;

    public SimpleItem(
            @NotNull Location location,
            @Nullable Model model,
            @Nullable ItemCategoryBase category
    ) {
        super(location);
        this.category = category;
        this.renderer = model != null ? new ModelRenderer<>(model) : null;

    }

    public SimpleItem(@NotNull Location location, @Nullable Model model) {
        this(location, model, null);
    }

    @Override
    public @Nullable StaticRenderer<Graphics3D, ItemUnit<?>> renderer() {
        return renderer;
    }

    @Override
    public @Nullable ItemCategoryBase category() {
        return category;
    }

}
