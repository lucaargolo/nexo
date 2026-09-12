package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.unit.Stackable;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.IntStream;

public interface Vault<U extends Unit<?>> extends Iterable<U> {

    @NotNull U empty();

    boolean canInsert();

    int insert(@NotNull U value, int max, boolean simulate);

    boolean canExtract();

    int extract(@NotNull U value, int max, boolean simulate);

    void clear();

    boolean isEmpty();

    int maxStackAmount();

    default void changed() {

    }

    interface Slotted<U extends Unit<?>> extends Vault<U> {

        int slots();

        @NotNull U get(int slot);

        @NotNull U set(int slot, @NotNull U value);

        @Override
        default boolean canInsert() {
            return true;
        }

        default boolean canInsert(int slot) {
            Objects.checkIndex(slot, this.slots());
            return this.canInsert();
        }

        default boolean canInsert(@NotNull U resource) {
            return this.canInsert();
        }

        default boolean canInsert(int slot, @NotNull U resource) {
            return this.canInsert(slot) && this.canInsert(resource);
        }

        @Override
        default int insert(@NotNull U value, int max, boolean simulate) {
            if (max < 0) {
                throw new IllegalArgumentException("Insertion amount cannot be negative");
            }
            if (max == 0 || !this.canInsert(value) || Objects.equals(value, this.empty())) {
                return 0;
            }

            int inserted = 0;
            if (value instanceof Stackable<?>) {
                for (int slot = 0; slot < this.slots() && inserted < max; slot++) {
                    U current = this.get(slot);
                    if (!this.isEmpty(slot) && Objects.equals(current, value)) {
                        inserted += this.insert(slot, value, max - inserted, simulate);
                    }
                }
            }

            for (int slot = 0; slot < this.slots() && inserted < max; slot++) {
                if (this.isEmpty(slot)) {
                    inserted += this.insert(slot, value, max - inserted, simulate);
                }
            }
            return inserted;
        }

        @SuppressWarnings("unchecked")
        default int insert(int slot, @NotNull U value, int max, boolean simulate) {
            Objects.checkIndex(slot, this.slots());
            if (max < 0) {
                throw new IllegalArgumentException("Insertion amount cannot be negative");
            }
            if (max == 0 || !this.canInsert(slot, value) || Objects.equals(value, this.empty())) {
                return 0;
            }

            U current = this.get(slot);
            if (this.isEmpty(slot)) {
                if (value instanceof Stackable<?> incoming) {
                    int amount = Math.min(max, this.maxStackAmount(slot, value));
                    if (amount <= 0) {
                        return 0;
                    }
                    if (!simulate) {
                        Stackable<?> result = incoming.copy();
                        int difference = amount - result.amount();
                        if (difference > 0) {
                            result.increment(result.divisor(), difference);
                        } else if (difference < 0) {
                            result.decrement(result.divisor(), -difference);
                        }
                        this.set(slot, (U) result);
                    }
                    return amount;
                }
                if (!simulate) {
                    this.set(slot, value);
                }
                return 1;
            }

            if (!Objects.equals(current, value) || !(current instanceof Stackable<?> stack)) {
                return 0;
            }

            int amount = Math.min(max, this.maxStackAmount(slot, value) - stack.amount());
            if (amount <= 0) {
                return 0;
            }
            if (!simulate) {
                Stackable<?> result = stack.copy();
                result.increment(result.divisor(), amount);
                this.set(slot, (U) result);
            }
            return amount;
        }

        @Override
        default boolean canExtract() {
            return true;
        }

        default boolean canExtract(int slot) {
            Objects.checkIndex(slot, this.slots());
            return this.canExtract();
        }

        @Override
        default int extract(@NotNull U value, int max, boolean simulate) {
            if (max < 0) {
                throw new IllegalArgumentException("Extraction amount cannot be negative");
            }
            if (max == 0 || !this.canExtract() || Objects.equals(value, this.empty())) {
                return 0;
            }

            int extracted = 0;
            for (int slot = 0; slot < this.slots() && extracted < max; slot++) {
                extracted += this.extract(slot, value, max - extracted, simulate);
            }
            return extracted;
        }

        @SuppressWarnings("unchecked")
        default int extract(int slot, @NotNull U value, int max, boolean simulate) {
            Objects.checkIndex(slot, this.slots());
            if (max < 0) {
                throw new IllegalArgumentException("Extraction amount cannot be negative");
            }
            if (max == 0 || !this.canExtract(slot) || Objects.equals(value, this.empty())) {
                return 0;
            }

            U current = this.get(slot);
            if (this.isEmpty(slot) || !Objects.equals(current, value)) {
                return 0;
            }
            if (current instanceof Stackable<?> stack) {
                int amount = Math.min(max, stack.amount());
                if (amount == 0) {
                    return 0;
                }
                if (!simulate) {
                    if (amount == stack.amount()) {
                        this.clear(slot);
                    } else {
                        Stackable<?> result = stack.copy();
                        result.decrement(result.divisor(), amount);
                        this.set(slot, (U) result);
                    }
                }
                return amount;
            }

            if (!simulate) {
                this.clear(slot);
            }
            return 1;
        }

        @Override
        default void clear() {
            for (int slot = 0; slot < this.slots(); slot++) {
                this.clear(slot);
            }
        }

        @Override
        default boolean isEmpty() {
            for (int slot = 0; slot < this.slots(); slot++) {
                if (!this.isEmpty(slot)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        default @NotNull Iterator<U> iterator() {
            return IntStream.range(0, this.slots())
                    .mapToObj(this::get)
                    .iterator();
        }

        default @NotNull U remove(int slot) {
            return this.remove(slot, Integer.MAX_VALUE);
        }

        @SuppressWarnings("unchecked")
        default @NotNull U remove(int slot, int amount) {
            Objects.checkIndex(slot, this.slots());
            if (amount < 0) {
                throw new IllegalArgumentException("Removal amount cannot be negative");
            }
            if (amount == 0 || !this.canExtract(slot)) {
                return this.empty();
            }

            U unit = this.get(slot);
            if (this.isEmpty(slot)) {
                return this.empty();
            }
            if (unit instanceof Stackable<?> stack) {
                int removedAmount = this.extract(slot, unit, amount, false);
                if (removedAmount == 0) {
                    return this.empty();
                }
                Stackable<?> removed = stack.copy();
                int difference = removed.amount() - removedAmount;
                if (difference > 0) {
                    removed.decrement(removed.divisor(), difference);
                } else if (difference < 0) {
                    removed.increment(removed.divisor(), -difference);
                }
                return (U) removed;
            }
            return this.extract(slot, unit, 1, false) == 1 ? unit : this.empty();
        }

        default @NotNull U clear(int slot) {
            Objects.checkIndex(slot, this.slots());
            if (!this.canExtract(slot)) {
                return this.empty();
            }
            return this.set(slot, this.empty());
        }

        default boolean isEmpty(int slot) {
            Objects.checkIndex(slot, this.slots());
            return Objects.equals(this.get(slot), this.empty());
        }

        default int maxStackAmount(int slot) {
            Objects.checkIndex(slot, this.slots());
            return this.maxStackAmount();
        }

        default int maxStackAmount(int slot, @NotNull U resource) {
            if (resource instanceof Stackable<?> stack) {
                return Math.min(this.maxStackAmount(slot), stack.maxAmount());
            }
            return Math.min(this.maxStackAmount(slot), 1);
        }
    }
}
