package dev.lucaargolo.nexo.api.stack;

import dev.lucaargolo.nexo.api.feature.IData;
import dev.lucaargolo.nexo.api.feature.IFeature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Stack<T extends IFeature> {

    private final Map<IData<?>, Object> dataMap = new ConcurrentHashMap<>();

    @Nullable
    private final T feature;

    public Stack(@NotNull T feature) {
        this.feature = feature;
    }

    public @Nullable T get() {
        return feature;
    }

    public boolean hasData(IData<?> data) {
        return dataMap.containsKey(data);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <D> D getData(IData<D> data) {
        return (D) dataMap.get(data);
    }

    public <D> void setData(IData<D> data, D d) {
        dataMap.put(data, d);
    }

}
