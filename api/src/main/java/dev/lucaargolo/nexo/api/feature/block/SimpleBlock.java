package dev.lucaargolo.nexo.api.feature.block;

import dev.lucaargolo.nexo.api.feature.item.BlockItem;
import dev.lucaargolo.nexo.api.model.Model;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.entity.EntityUnit;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Interaction;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

public class SimpleBlock extends BlockBase {

    @Nullable
    private final Model model;
    @Nullable
    private final BlockItem item;

    public SimpleBlock(@NotNull Location location, @Nullable Model model, @Nullable BlockItem item) {
        super(location);
        this.model = model;
        this.item = item;
    }

    public SimpleBlock(@NotNull Location location, @Nullable Model model) {
        this(location, model, null);
    }

    @Override
    public @Nullable Model model() {
        return model;
    }

    @Override
    public @Nullable BlockItem item() {
        return item;
    }

    @Override
    public @NotNull Interaction onInteract(@NotNull BlockUnit<?> block, @NotNull WorldUnit<?> world, @NotNull EntityUnit<PlayerRole> entity, @NotNull Vector3i pos) {
        return Interaction.PASS;
    }

}
