package dev.lucaargolo.nexo.api.feature.item;

import dev.lucaargolo.nexo.api.feature.IFeature;
import org.jetbrains.annotations.NotNull;

public interface IItemCategory extends IFeature {

    @Override
    default @NotNull Type type() {
        return Type.ITEM_CATEGORY;
    };
}
