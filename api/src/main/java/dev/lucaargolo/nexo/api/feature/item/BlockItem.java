package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.role.BlockItemRole;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockItem extends ItemBase {

    @Nullable
    private final Model model;
    @Nullable
    private final ItemCategoryBase category;
    public BlockItem(
            @NotNull Location location,
            @Nullable Model model,
            @Nullable ItemCategoryBase category,
            @NotNull BlockBase block
    ) {
        super(location, new BlockItemRole(block));
        this.model = model;
        this.category = category;
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
