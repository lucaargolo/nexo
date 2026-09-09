package dev.lucaargolo.nexo.api.unit;

public interface Stackable<T> extends Copyable<Stackable<T>> {

    int maxAmount();

    int amount();

    void increment(int amount);

    void decrement(int amount);

}
