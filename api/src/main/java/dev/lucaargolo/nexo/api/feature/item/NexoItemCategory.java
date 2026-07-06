package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.Feature;
import org.jetbrains.annotations.NotNull;

public abstract class NexoItemCategory extends Feature<NexoItemCategory> {

    @Override
    @NotNull
    public final Class<NexoItemCategory> type() {
        return NexoItemCategory.class;
    }

}
