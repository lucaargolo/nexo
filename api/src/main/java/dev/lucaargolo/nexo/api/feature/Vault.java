package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface Vault<U extends Unit<?>> extends List<U> {

    @NotNull U defaultValue();

    boolean isFull();

    default void contentsChanged() {
    }

    default void setContents(@NotNull Collection<? extends U> contents) {
        List<? extends U> copy = new ArrayList<>(contents);
        if (copy.size() > this.size()) {
            throw new IllegalArgumentException("Vault contents exceed fixed size of " + this.size());
        }
        this.clear();
        for (int index = 0; index < copy.size(); index++) {
            this.set(index, copy.get(index));
        }
    }

    default boolean canAdd() {
        return true;
    }

    default boolean canRemove() {
        return true;
    }

}
