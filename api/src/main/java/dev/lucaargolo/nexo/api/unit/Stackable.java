package dev.lucaargolo.nexo.api.unit;

public interface Stackable<S extends Stackable<S>> extends Copyable<S> {

    int maxAmount();

    int amount();

    void increment(int amount);

    void decrement(int amount);
}
