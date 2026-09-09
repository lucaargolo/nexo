package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.unit.Stackable;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.IntStream;

public interface Vault<U extends Unit<?>> extends Iterable<U> {

    @NotNull U empty();

    int slots();

    @NotNull U get(int slot);

    @NotNull U set(int slot, @NotNull U value);

    default @NotNull U remove(int slot) {
        U unit = this.get(slot);
        this.clear(slot);
        return unit;
    }

    default @NotNull U remove(int slot, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Removal amount cannot be negative");
        }
        if (amount == 0) {
            return this.empty();
        }

        U unit = this.get(slot);
        if (unit instanceof Stackable<?> stack) {
            if (amount >= stack.amount()) {
                this.clear(slot);
                return unit;
            } else {
                Stackable<?> removed = stack.copy();
                int remaining = stack.amount() - amount;

                stack.decrement(amount);
                this.set(slot, unit);
                removed.decrement(remaining);

                return (U) removed;
            }
        } else {
            this.clear(slot);
            return unit;
        }
    }

    default @NotNull U clear(int slot) {
        return this.set(slot, this.empty());
    }

    default void clear() {
        for (int slot = 0; slot < this.slots(); slot++) {
            this.clear(slot);
        }
    }

    default boolean isEmpty(int slot) {
        return Objects.equals(this.get(slot), this.empty());
    }

    default boolean isEmpty() {
        for (int slot = 0; slot < this.slots(); slot++) {
            if (!this.isEmpty(slot)) {
                return false;
            }
        }
        return true;
    }

    default boolean canAdd() {
        return true;
    }

    default boolean canAdd(int slot) {
        return this.canAdd();
    }

    default boolean canAdd(@NotNull U resource) {
        return this.canAdd();
    }

    default boolean canAdd(int slot, @NotNull U resource) {
        return this.canAdd(slot) && this.canAdd(resource);
    }

    default boolean canRemove() {
        return true;
    }

    default boolean canRemove(int slot) {
        return this.canRemove();
    }

    default void changed() {

    }

    default int insert(@NotNull U value, int max, boolean simulate) {
        if (max < 0) {
            throw new IllegalArgumentException("Insertion amount cannot be negative");
        }
        if (max == 0 || !this.canAdd(value)) {
            return 0;
        }
        if (Objects.equals(value, this.empty())) {
            return 0;
        }

        int inserted = 0;

        if (value instanceof Stackable<?> incoming) {
            // Fill compatible existing stacks first.
            for (int slot = 0; slot < this.slots() && inserted < max; slot++) {
                if (!this.canAdd(slot, value)) {
                    continue;
                }

                U current = this.get(slot);

                if (this.isEmpty(slot) || !Objects.equals(current, value) || !(current instanceof Stackable<?> stack)) {
                    continue;
                }

                int available = stack.maxAmount() - stack.amount();
                if (available <= 0) {
                    continue;
                }

                int amount = Math.min(max - inserted, available);

                if (!simulate) {
                    Stackable<?> result = stack.copy();
                    result.increment(amount);
                    this.set(slot, (U) result);
                }

                inserted += amount;
            }

            // Put the remainder into empty slots.
            for (int slot = 0; slot < this.slots() && inserted < max; slot++) {
                if (!this.isEmpty(slot) || !this.canAdd(slot, value)) {
                    continue;
                }

                int amount = Math.min(max - inserted, incoming.maxAmount());
                if (amount <= 0) {
                    continue;
                }

                if (!simulate) {
                    Stackable<?> result = incoming.copy();
                    int difference = amount - result.amount();

                    if (difference > 0) {
                        result.increment(difference);
                    } else if (difference < 0) {
                        result.decrement(-difference);
                    }

                    this.set(slot, (U) result);
                }

                inserted += amount;
            }
        } else {
            // Non-stackable units occupy one slot each.
            for (int slot = 0; slot < this.slots() && inserted < max; slot++) {
                if (!this.isEmpty(slot) || !this.canAdd(slot, value)) {
                    continue;
                }

                if (!simulate) {
                    this.set(slot, value);
                }

                inserted++;
            }
        }

        return inserted;
    }

    default int extract(@NotNull U value, int max, boolean simulate) {
        if (max < 0) {
            throw new IllegalArgumentException("Extraction amount cannot be negative");
        }
        if (max == 0 || !this.canRemove()) {
            return 0;
        }
        if (Objects.equals(value, this.empty())) {
            return 0;
        }

        int extracted = 0;

        for (int slot = 0; slot < this.slots() && extracted < max; slot++) {
            if (!this.canRemove(slot)) {
                continue;
            }

            U current = this.get(slot);

            if (this.isEmpty(slot) || !Objects.equals(current, value)) {
                continue;
            }

            if (current instanceof Stackable<?> stack) {
                int amount = Math.min(max - extracted, stack.amount());

                if (!simulate) {
                    if (amount >= stack.amount()) {
                        this.clear(slot);
                    } else {
                        Stackable<?> result = stack.copy();
                        result.decrement(amount);
                        this.set(slot, (U) result);
                    }
                }

                extracted += amount;
            } else {
                if (!simulate) {
                    this.clear(slot);
                }

                extracted++;
            }
        }

        return extracted;
    }

    @Override
    default @NotNull Iterator<U> iterator() {
        return IntStream.range(0, this.slots())
                .mapToObj(this::get)
                .iterator();
    }

}