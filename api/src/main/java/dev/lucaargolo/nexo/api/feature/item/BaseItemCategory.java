package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.Feature;
import org.jetbrains.annotations.NotNull;

public abstract class BaseItemCategory extends Feature<BaseItemCategory> {

    @Override
    @NotNull
    public Class<BaseItemCategory> type() {
        return BaseItemCategory.class;
    }

}
