package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class StringData extends NexoData<String> {

    @NotNull
    private final Location location;

    public StringData(@NotNull Location location) {
        this.location = location;
    }

    @Override
    @NotNull
    public Location location() {
        return location;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull String data) {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + bytes.length);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull String read(@NotNull ByteBuffer buffer) {
        int length = buffer.getInt();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull String data) {
        return new JsonPrimitive(data);
    }

    @Override
    public @NotNull String deserialize(@NotNull JsonElement element) {
        return element.getAsString();
    }

}
