package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.feature.provider.IItemProvider;
import dev.lucaargolo.nexo.api.feature.provider.IModelProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IItem extends IFeature<IItem>, IModelProvider, IItemProvider {

    @Override
    @NotNull default Class<IItem> type() {
        return IItem.class;
    }

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