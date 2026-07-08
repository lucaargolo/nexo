package dev.lucaargolo.nexo.api.feature;

import dev.lucaargolo.nexo.api.feature.data.NexoData;
import dev.lucaargolo.nexo.api.util.Side;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Instance<T extends Feature<T>> {

    @NotNull
    private final UUID uuid = UUID.randomUUID();
    @NotNull
    private final Map<NexoData<?>, Object> dataMap = new ConcurrentHashMap<>();

    @Nullable
    private final T feature;
    @NotNull
    private final Side side;

    public Instance(@Nullable T feature, @NotNull Side side) {
        this.feature = feature;
        this.side = side;
    }

    public @NotNull UUID id() {
        return uuid;
    }

    public @Nullable T get() {
        return feature;
    }

    public @NotNull Side side() {
        return side;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <D> D getData(@NotNull NexoData<D> data) {
        return (D) dataMap.get(data);
    }

    public <D> void setData(@NotNull NexoData<D> data, @Nullable D d) {
        dataMap.put(data, d);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Instance<?> instance = (Instance<?>) o;
        return Objects.equals(uuid, instance.uuid) && Objects.equals(dataMap, instance.dataMap) && Objects.equals(feature, instance.feature) && side == instance.side;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, dataMap, feature, side);
    }

}
