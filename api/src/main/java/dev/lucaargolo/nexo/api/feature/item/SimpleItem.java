package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.model.ModelRenderer;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Function;

public class SimpleItem extends ItemBase {


    private final @Nullable Renderer<Graphics3D, ItemUnit> renderer;
    private final @Nullable ItemCategoryBase category;

    public SimpleItem(
            @Nullable ModelResource resource,
            @Nullable ItemCategoryBase category
    ) {
        this.renderer = resource != null ? new ModelRenderer<>(resource) : null;
        this.category = category;
    }

    public SimpleItem(
            @NotNull Renderer<Graphics3D, ItemUnit> renderer,
            @Nullable ItemCategoryBase category
    ) {
        this.renderer = renderer;
        this.category = category;
    }

    @Override
    public @Nullable Renderer<Graphics3D, ItemUnit> renderer() {
        return renderer;
    }

    @Override
    public @Nullable ItemCategoryBase category() {
        return category;
    }

    @Override
    public @NotNull <V extends Unit<?>> Set<String> vaults(@NotNull Class<V> type) {
        return Set.of();
    }

    @Override
    public @Nullable <V extends Unit<?>> Function<ItemUnit, Vault<V>> vault(@NotNull Class<V> type, @NotNull String key) {
        return null;
    }

}
