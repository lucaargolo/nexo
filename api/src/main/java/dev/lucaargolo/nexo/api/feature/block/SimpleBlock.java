package dev.lucaargolo.nexo.api.feature.block;

import dev.lucaargolo.nexo.api.feature.item.BlockItem;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleBlock extends BlockBase {

    @NotNull
    private final Location location;
    @Nullable
    private final Model model;
    @Nullable
    private final BlockItem item;

    public SimpleBlock(@NotNull Location location, @Nullable Model model, @Nullable BlockItem item) {
        this.location = location;
        this.model = model;
        this.item = item;
    }

    public SimpleBlock(@NotNull Location location, @Nullable Model model) {
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
    @Nullable
    public BlockItem item() {
        return item;
    }

}
