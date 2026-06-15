package dev.lucaargolo.nexo.api;

import dev.lucaargolo.nexo.api.feature.Feature;
import org.jetbrains.annotations.Nullable;

public interface Nexo {

    @Nullable <T extends Feature> T get(Class<T> type, Identifier id);

    @Nullable <T extends Feature> T add(Identifier id, T feature);

}
