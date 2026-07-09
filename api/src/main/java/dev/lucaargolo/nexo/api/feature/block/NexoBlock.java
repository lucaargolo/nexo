package dev.lucaargolo.nexo.api.feature.block;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.ItemProvider;
import dev.lucaargolo.nexo.api.feature.ModelProvider;
import dev.lucaargolo.nexo.api.instance.world.WorldInstance;
import dev.lucaargolo.nexo.api.util.Interaction;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;

public abstract class NexoBlock extends Feature<NexoBlock> implements ModelProvider, ItemProvider {

    @Override
    @NotNull
    public final Class<NexoBlock> type() {
        return NexoBlock.class;
    }

    @NotNull
    public Interaction onInteract(@NotNull WorldInstance world, @NotNull Vector3i pos) {
        return Interaction.PASS;
    }

}
