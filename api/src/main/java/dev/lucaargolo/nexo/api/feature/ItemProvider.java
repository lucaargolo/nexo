package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.feature.item.NexoItem;
import org.jetbrains.annotations.Nullable;

public interface ItemProvider {

    @Nullable NexoItem item();

}
