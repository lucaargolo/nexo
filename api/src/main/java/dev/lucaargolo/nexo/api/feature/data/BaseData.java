package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import dev.lucaargolo.nexo.api.feature.Feature;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public abstract class BaseData<D> extends Feature<BaseData<D>> {

    @Override
    @SuppressWarnings("unchecked")
    @NotNull
    public Class<BaseData<D>> type() {
        return (Class<BaseData<D>>) (Class<?>) BaseData.class;
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
