package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import dev.lucaargolo.nexo.api.feature.IFeature;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public interface IData<D> extends IFeature {

    @NotNull
    CountData COUNT = new CountData(Location.of("nexo", "count"));

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
