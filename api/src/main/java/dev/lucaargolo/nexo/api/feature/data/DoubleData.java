package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import java.nio.ByteBuffer;

public final class DoubleData extends DataBase<Double> {

    @NotNull
    private final Double initial;

    public DoubleData(@NotNull Location location, @NotNull Double initial) {
        super(location);
        this.initial = initial;
    }

    @Override
    @NotNull
    public Double initial() {
        return this.initial;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Double data) {
        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        buffer.putDouble(data);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Double read(@NotNull ByteBuffer buffer) {
        return buffer.getDouble();
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Double data) {
        return new JsonPrimitive(data);
    }

    @Override
    public @NotNull Double deserialize(@NotNull JsonElement element) {
        return element.getAsDouble();
    }

}
