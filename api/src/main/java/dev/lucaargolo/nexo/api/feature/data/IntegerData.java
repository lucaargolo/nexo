package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class IntegerData extends DataBase<Integer> {

    @NotNull
    private final Integer initial;

    public IntegerData(@NotNull Location location, @NotNull Integer initial) {
        super(location);
        this.initial = initial;
    }

    @Override
    @NotNull
    public Integer initial() {
        return this.initial;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Integer data) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(data);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Integer read(@NotNull ByteBuffer buffer) {
        return buffer.getInt();
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Integer data) {
        return new JsonPrimitive(data);
    }

    @Override
    public @NotNull Integer deserialize(@NotNull JsonElement element) {
        return element.getAsInt();
    }

}
