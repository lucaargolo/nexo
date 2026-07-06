package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.provider.ItemProvider;
import dev.lucaargolo.nexo.api.feature.provider.ModelProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class NexoItem extends Feature<NexoItem> implements ModelProvider, ItemProvider {

    @Override
    @NotNull
    public final Class<NexoItem> type() {
        return NexoItem.class;
    }

    @Override
    @NotNull
    public NexoItem item() {
        return this;
    }

    @Nullable
    public NexoItemCategory category() {
        return null;
    }

}
