package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class StringData extends DataBase<String> {

    @NotNull
    private final String initial;

    public StringData(@NotNull Location location, @NotNull String initial) {
        super(location);
        this.initial = initial;
    }

    @Override
    public @NotNull String initial() {
        return initial;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + bytes.length);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull String read(@NotNull ByteBuffer buffer) {
        int length = buffer.getInt();
        if (length < 0 || length > buffer.remaining()) {
            throw new IllegalArgumentException("Invalid string length: " + length);
        }
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull String value) {
        return new JsonPrimitive(value);
    }

    @Override
    public @NotNull String deserialize(@NotNull JsonElement element) {
        return element.getAsString();
    }

}
