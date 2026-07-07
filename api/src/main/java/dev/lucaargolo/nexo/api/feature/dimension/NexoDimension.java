package dev.lucaargolo.nexo.api.feature.dimension;

import dev.lucaargolo.nexo.api.feature.Feature;
import org.jetbrains.annotations.NotNull;

public abstract class NexoDimension extends Feature<NexoDimension> {

    @Override
    @NotNull
    public final Class<NexoDimension> type() {
        return NexoDimension.class;
    }

}
