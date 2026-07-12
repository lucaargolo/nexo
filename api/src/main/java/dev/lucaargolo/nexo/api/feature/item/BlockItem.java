package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.component.BlockItemComponent;
import dev.lucaargolo.nexo.api.component.Component;
import dev.lucaargolo.nexo.api.feature.block.BlockBase;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlockItem extends ItemBase {

    @NotNull
    private final Location location;
    @Nullable
    private final Model model;
    @Nullable
    private final ItemCategoryBase category;
    @NotNull
    private final BlockBase block;

    public BlockItem(
            @NotNull Location location,
            @Nullable Model model,
            @Nullable ItemCategoryBase category,
            @NotNull BlockBase block
    ) {
        this.location = location;
        this.model = model;
        this.category = category;
        this.block = block;
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
    public @Nullable ItemCategoryBase category() {
        return category;
    }

    @Override
    public @NotNull List<@NotNull Component> components() {
        return List.of(new BlockItemComponent(block));
    }

}
