package dev.lucaargolo.nexo.api.feature;

import org.jetbrains.annotations.Nullable;

public interface IBlock extends IFeature, IModelProvider {

    @Nullable IItem item();

}
