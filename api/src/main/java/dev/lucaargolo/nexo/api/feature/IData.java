package dev.lucaargolo.nexo.api.feature;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface IData<D> extends IFeature {

    @NotNull ByteBuffer write(@NotNull D data);

    @NotNull D read(@NotNull ByteBuffer buffer);

    @NotNull JsonElement serialize(@NotNull D data);

    @NotNull D deserialize(@NotNull JsonElement element);

    default boolean persistent() {
        return true;
    }

    default boolean synced() {
        return true;
    }

}
