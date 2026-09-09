package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.unit.Stackable;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.IntStream;

public interface Vault<U extends Unit<?>> extends Iterable<U> {

    @NotNull U empty();

    int size();

    @NotNull U get(int slot);

    @NotNull U set(int slot, @NotNull U value);

    default @NotNull U remove(int slot) {
        U unit = this.get(slot);
        this.clear(slot);
        return unit;
    }

    default @NotNull U remove(int slot, int amount) {
        U unit = this.get(slot);
        if(unit instanceof Stackable<?> stack) {
            if(amount >= stack.amount()) {
                this.clear(slot);
                return unit;
            }else{
                Stackable<?> removed = stack.copy();
                int excedent = stack.amount() - amount;
                stack.decrement(excedent);
                removed.decrement(amount);
                return (U) removed;
            }
        }else {
            this.clear(slot);
            return unit;
        }
    }

    default @NotNull U clear(int slot) {
        return set(slot, empty());
    }

    default void clear() {
        for (int slot = 0; slot < size(); slot++) {
            clear(slot);
        }
    }

    default boolean isEmpty(int slot) {
        return Objects.equals(this.get(slot), this.empty());
    }

    default boolean isEmpty() {
        for (int slot = 0; slot < size(); slot++) {
            if(!isEmpty(slot)) {
                return false;
            }
        }
        return true;
    }

    default boolean canAdd() {
        return true;
    }

    default boolean canRemove() {
        return true;
    }

    default void changed() {
    }

    @Override
    default @NotNull Iterator<U> iterator() {
        return IntStream.range(0, this.size()).mapToObj(this::get).iterator();
    };
}
