package dev.lucaargolo.nexo.api.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class CountData implements IData<Integer> {

    private final @NotNull Location location;

    public CountData(@NotNull Location location) {
        this.location = location;
    }

    @Override
    public @NotNull Location location() {
        return location;
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
