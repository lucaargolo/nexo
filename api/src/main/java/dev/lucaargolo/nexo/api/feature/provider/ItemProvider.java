package dev.lucaargolo.nexo.api.feature.provider;

import dev.lucaargolo.nexo.api.feature.item.BaseItem;
import org.jetbrains.annotations.Nullable;

public interface ItemProvider {

    @Nullable BaseItem item();

}
