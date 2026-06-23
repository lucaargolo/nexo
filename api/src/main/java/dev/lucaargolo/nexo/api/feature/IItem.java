package dev.lucaargolo.nexo.api.feature;

import org.jetbrains.annotations.Nullable;

public interface IItem extends IFeature, IModelProvider {

    @Nullable
    default IItemCategory category() {
        return null;
    }

}