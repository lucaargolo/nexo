package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import java.nio.ByteBuffer;

public final class FloatData extends DataBase<Float> {

    @NotNull
    private final Float initial;
    @NotNull
    private final Location location;

    public FloatData(@NotNull Float initial, @NotNull Location location) {
        this.initial = initial;
        this.location = location;
    }

    @Override
    public Float initial() {
        return this.initial;
    }

    @Override
    @NotNull
    public Location location() {
        return location;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Float data) {
        ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES);
        buffer.putFloat(data);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Float read(@NotNull ByteBuffer buffer) {
        return buffer.getFloat();
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Float data) {
        return new JsonPrimitive(data);
    }

    @Override
    public @NotNull Float deserialize(@NotNull JsonElement element) {
        return element.getAsFloat();
    }

}
