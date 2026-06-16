package dev.lucaargolo.nexo.api;

import dev.lucaargolo.nexo.api.feature.IFeature;
import org.jetbrains.annotations.Nullable;

public interface Nexo {

    @Nullable <T extends IFeature> T getFeature(Class<T> type, Location location);

    @Nullable <T extends IFeature, I extends T> T registerFeature(Class<T> type, Location location, I feature);

}
