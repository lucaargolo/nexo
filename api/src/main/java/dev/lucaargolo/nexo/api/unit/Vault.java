package dev.lucaargolo.nexo.api.unit;

import java.util.Collection;

public interface Vault<U extends Unit<?, ?>> extends Collection<U> {

    boolean isFull();

}
