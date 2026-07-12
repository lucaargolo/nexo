package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import java.nio.ByteBuffer;

public final class FloatData extends DataBase<Float> {

    @NotNull
    private final Float initial;

    public FloatData(@NotNull Location location, @NotNull Float initial) {
        super(location);
        this.initial = initial;
    }

    @Override
    @NotNull
    public Float initial() {
        return this.initial;
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
