package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;

public interface Vault<U extends Unit<?>> extends Iterable<U> {

    @NotNull U empty();

    boolean canAdd();

    int insert(@NotNull U value, int max, boolean simulate);

    boolean canRemove();

    int extract(@NotNull U value, int max, boolean simulate);

    void clear();

    boolean isEmpty();

    default void changed() {

    }

}