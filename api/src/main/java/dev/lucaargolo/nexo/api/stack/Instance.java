package dev.lucaargolo.nexo.api.stack;

import dev.lucaargolo.nexo.api.feature.IData;
import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.feature.IItem;
import dev.lucaargolo.nexo.api.feature.IItemProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Instance<T extends IFeature> {

    @NotNull
    private final Map<IData<?>, Object> dataMap = new ConcurrentHashMap<>();

    @Nullable
    private final T feature;

    public Instance(@Nullable T feature) {
        this.feature = feature;
    }

    public @Nullable T get() {
        Integer count = getData(IData.COUNT);
        if (count != null && count == 0) {
            return null;
        }
        return feature;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <D> D getData(@NotNull IData<D> data) {
        return (D) dataMap.get(data);
    }

    public <D> void setData(@NotNull IData<D> data, D d) {
        dataMap.put(data, d);
    }
    public int getCount() {
        Integer count = getData(IData.COUNT);
        if (count == null) {
            throw new IllegalStateException("CountData is not present in this stack");
        }
        return count;
    }

    public void setCount(int count) {
        if (getData(IData.COUNT) == null) {
            throw new IllegalStateException("CountData is not present in this stack");
        }
        setData(IData.COUNT, count);
    }

    public static @NotNull Instance<IItem> item(@NotNull IItemProvider item, int count) {
        IItem i = item.item();
        if (i == null) {
            throw new IllegalArgumentException("Item provider returned null item");
        }
        Instance<IItem> instance = new Instance<>(i);
        instance.setData(IData.COUNT, count);
        return instance;
    }

}
