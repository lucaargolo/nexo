package dev.lucaargolo.nexo.api.feature.provider;

import dev.lucaargolo.nexo.api.feature.item.IItem;
import org.jetbrains.annotations.Nullable;

public interface IItemProvider {

    @Nullable IItem item();

}
