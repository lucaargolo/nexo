package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.component.BlockItemComponent;
import dev.lucaargolo.nexo.api.component.Component;
import dev.lucaargolo.nexo.api.feature.block.BaseBlock;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlockItem extends BaseItem {

    @NotNull
    private final Location location;
    @Nullable
    private final Model model;
    @Nullable
    private final BaseItemCategory category;
    @NotNull
    private final BaseBlock block;

    public BlockItem(
            @NotNull Location location,
            @Nullable Model model,
            @Nullable BaseItemCategory category,
            @NotNull BaseBlock block
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
    public @Nullable BaseItemCategory category() {
        return category;
    }

    @Override
    public @NotNull List<@NotNull Component> components() {
        return List.of(new BlockItemComponent(block));
    }

}
