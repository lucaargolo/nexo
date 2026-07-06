package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.feature.data.NexoData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Instance<T extends Feature<T>> {

    @NotNull
    private final Map<NexoData<?>, Object> dataMap = new ConcurrentHashMap<>();

    @Nullable
    private final T feature;

    public Instance(@Nullable T feature) {
        this.feature = feature;
    }

    public @Nullable T get() {
        return feature;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <D> D getData(@NotNull NexoData<D> data) {
        return (D) dataMap.get(data);
    }

    public <D> void setData(@NotNull NexoData<D> data, @Nullable D d) {
        dataMap.put(data, d);
    }

}
