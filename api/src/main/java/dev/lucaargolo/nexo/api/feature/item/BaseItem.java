package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.provider.ItemProvider;
import dev.lucaargolo.nexo.api.feature.provider.ModelProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseItem extends Feature<BaseItem> implements ModelProvider, ItemProvider {

    @Override
    @NotNull
    public Class<BaseItem> type() {
        return BaseItem.class;
    }

    @Override
    @NotNull
    public BaseItem item() {
        return this;
    }

    @Nullable
    public BaseItemCategory category() {
        return null;
    }

}
