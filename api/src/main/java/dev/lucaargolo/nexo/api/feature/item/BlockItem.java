package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.render.model.ModelRenderer;
import dev.lucaargolo.nexo.api.role.item.BlockItemRole;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockItem extends ItemBase {

    private final @Nullable ItemCategoryBase category;
    private final @Nullable StaticRenderer<Graphics3D, ItemUnit<?>> renderer;

    public BlockItem(
            @NotNull Location location,
            @Nullable Model model,
            @Nullable ItemCategoryBase category,
            @NotNull BlockBase block
    ) {
        super(location, () -> new BlockItemRole(block));
        this.renderer = model != null ? new ModelRenderer<>(model) : null;
        this.category = category;
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
