package dev.lucaargolo.nexo.api.event;

public interface IEvent<T> {

    T value();

    boolean cancelable();

    enum Priority {
        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST
    }

}
