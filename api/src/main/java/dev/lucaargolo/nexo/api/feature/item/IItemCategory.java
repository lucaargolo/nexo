package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.IFeature;
import org.jetbrains.annotations.NotNull;

public interface IItemCategory extends IFeature<IItemCategory> {

    @Override
    @NotNull default Class<IItemCategory> type() {
        return IItemCategory.class;
    }

}
