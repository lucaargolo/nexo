package dev.lucaargolo.nexo.api.feature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IItem extends IFeature, IModelProvider, IItemProvider {

    @Override
    @NotNull
    default IItem item() {
        return this;
    }

    @Nullable
    default IItemCategory category() {
        return null;
    }

}