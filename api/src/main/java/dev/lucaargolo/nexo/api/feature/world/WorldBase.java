package dev.lucaargolo.nexo.api.feature.world;

import dev.lucaargolo.nexo.api.feature.Feature;
import org.jetbrains.annotations.NotNull;

public abstract class WorldBase extends Feature<WorldBase> {

    @Override
    @NotNull
    public final Class<WorldBase> type() {
        return WorldBase.class;
    }

}
