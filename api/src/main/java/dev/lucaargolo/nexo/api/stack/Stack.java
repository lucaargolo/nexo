package dev.lucaargolo.nexo.api.stack;

import dev.lucaargolo.nexo.api.feature.IData;
import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.feature.IItem;
import dev.lucaargolo.nexo.api.feature.IItemProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Stack<T extends IFeature> {

    private final Map<IData<?>, Object> dataMap = new ConcurrentHashMap<>();

    @Nullable
    private final T feature;

    public Stack(@NotNull T feature) {
        this.feature = feature;
    }

    public @Nullable T get() {
        if (hasData(IData.COUNT) && getData(IData.COUNT) == 0) {
            return null;
        }
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

    public int getCount() {
        if (!hasData(IData.COUNT)) {
            throw new IllegalStateException("CountData is not present in this stack");
        }
        return getData(IData.COUNT);
    }

    public void setCount(int count) {
        if (!hasData(IData.COUNT)) {
            throw new IllegalStateException("CountData is not present in this stack");
        }
        setData(IData.COUNT, count);
    }

    public static Stack<IItem> item(IItemProvider item, int count) {
        Stack<IItem> stack = new Stack<>(item.item());
        stack.setData(IData.COUNT, count);
        return stack;
    }

}
