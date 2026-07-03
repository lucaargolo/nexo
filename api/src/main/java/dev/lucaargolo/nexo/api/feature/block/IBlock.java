package dev.lucaargolo.nexo.api.feature.block;

import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.feature.provider.IModelProvider;
import org.jetbrains.annotations.NotNull;

public interface IBlock extends IFeature, IModelProvider {

    @Override
    default @NotNull Type type() {
        return Type.BLOCK;
    }

}
