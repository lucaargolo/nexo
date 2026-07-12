package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import org.jetbrains.annotations.Nullable;

public interface ItemProvider {

    @Nullable ItemBase item();

}
