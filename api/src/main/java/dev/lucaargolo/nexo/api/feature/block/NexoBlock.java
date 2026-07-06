package dev.lucaargolo.nexo.api.feature.block;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.provider.ItemProvider;
import dev.lucaargolo.nexo.api.feature.provider.ModelProvider;
import org.jetbrains.annotations.NotNull;

public abstract class NexoBlock extends Feature<NexoBlock> implements ModelProvider, ItemProvider {

    @Override
    @NotNull
    public final Class<NexoBlock> type() {
        return NexoBlock.class;
    }

}
