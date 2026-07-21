package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.render.model.ModelRenderer;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class SimpleItem extends ItemBase {

    private final @Nullable ItemCategoryBase category;
    private final @Nullable Renderer<Graphics3D, ItemUnit<?>> renderer;

    public SimpleItem(
            @NotNull Location location,
            @Nullable ModelResource resource,
            @Nullable ItemCategoryBase category
    ) {
        super(location);
        this.category = category;
        this.renderer = resource != null ? new ModelRenderer<>(resource) : null;
    }

    public SimpleItem(@NotNull Location location, @Nullable ModelResource resource) {
        this(location, resource, null);
    }

    public SimpleItem(
            @NotNull Location location,
            @Nullable ItemCategoryBase category,
            @NotNull Renderer<Graphics3D, ItemUnit<?>> renderer
    ) {
        super(location);
        this.category = category;
        this.renderer = renderer;
    }

    public SimpleItem(
            @NotNull Location location,
            @NotNull Renderer<Graphics3D, ItemUnit<?>> renderer
    ) {
        this(location, null, renderer);
    }

    @Override
    public @Nullable Renderer<Graphics3D, ItemUnit<?>> renderer() {
        return renderer;
    }

    @Override
    public @Nullable ItemCategoryBase category() {
        return category;
    }

}
