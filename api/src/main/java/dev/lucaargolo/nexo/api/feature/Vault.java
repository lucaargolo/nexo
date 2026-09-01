package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface Vault<U extends Unit<?, ?>> extends Collection<U> {

    boolean isFull();

    default void contentsChanged() {
    }

    default void setContents(@NotNull Collection<? extends U> contents) {
        this.clear();
        this.addAll(contents);
    }

    default boolean canAdd() {
        return true;
    }

    default boolean canRemove() {
        return true;
    }

}
