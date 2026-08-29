package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.unit.Unit;

import java.util.Collection;

public interface Vault<U extends Unit<?, ?>> extends Collection<U> {

    boolean isFull();

}
