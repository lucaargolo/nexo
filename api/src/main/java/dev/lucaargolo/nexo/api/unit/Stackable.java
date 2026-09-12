package dev.lucaargolo.nexo.api.unit;

public interface Stackable<S extends Stackable<S>> extends Copyable<S> {

    int maxAmount();

    int amount();

    void increment(int amount);

    default void increment(int divisor, int amount) {
        if (divisor != 1) {
            throw new IllegalArgumentException("This stackable does not support fractional amounts");
        }
        this.increment(amount);
    }

    void decrement(int amount);

    default void decrement(int divisor, int amount) {
        if (divisor != 1) {
            throw new IllegalArgumentException("This stackable does not support fractional amounts");
        }
        this.decrement(amount);
    }

    default int divisor() {
        return 1;
    }

}
