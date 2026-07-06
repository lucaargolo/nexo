package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import dev.lucaargolo.nexo.api.feature.Feature;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public abstract class NexoData<D> extends Feature<NexoData<D>> {

    @Override
    @SuppressWarnings("unchecked")
    @NotNull
    public final Class<NexoData<D>> type() {
        return (Class<NexoData<D>>) (Class<?>) NexoData.class;
    }

    @NotNull
    public abstract ByteBuffer write(@NotNull D data);

    @NotNull
    public abstract D read(@NotNull ByteBuffer buffer);

    @NotNull
    public abstract JsonElement serialize(@NotNull D data);

    @NotNull
    public abstract D deserialize(@NotNull JsonElement element);

    public boolean persistent() {
        return true;
    }

    public boolean synced() {
        return true;
    }

}
