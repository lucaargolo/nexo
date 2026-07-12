package dev.lucaargolo.nexo.api.feature.block;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.ItemProvider;
import dev.lucaargolo.nexo.api.feature.ModelProvider;
import dev.lucaargolo.nexo.api.unit.world.WorldUnit;
import dev.lucaargolo.nexo.api.util.Interaction;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;

public abstract class BlockBase extends Feature<BlockBase> implements ModelProvider, ItemProvider {

    public BlockBase(@NotNull Location location) {
        super(location);
    }

    @NotNull
    public Interaction onInteract(@NotNull WorldUnit world, @NotNull Vector3i pos) {
        return Interaction.PASS;
    }

}
