package dev.lucaargolo.nexo.api.feature.world;

import dev.lucaargolo.nexo.api.feature.Feature;
import org.jetbrains.annotations.NotNull;

public abstract class NexoWorld extends Feature<NexoWorld> {

    @Override
    @NotNull
    public final Class<NexoWorld> type() {
        return NexoWorld.class;
    }

}
