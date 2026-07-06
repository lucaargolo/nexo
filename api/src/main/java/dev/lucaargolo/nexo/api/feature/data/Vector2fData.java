package dev.lucaargolo.nexo.api.feature.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.nio.ByteBuffer;

public final class Vector2fData extends NexoData<Vector2f> {

    private static final int BYTES = Float.BYTES * 2;

    @NotNull
    private final Location location;

    public Vector2fData(@NotNull Location location) {
        this.location = location;
    }

    @Override
    @NotNull
    public Location location() {
        return location;
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull Vector2f data) {
        ByteBuffer buffer = ByteBuffer.allocate(BYTES);
        buffer.putFloat(data.x);
        buffer.putFloat(data.y);
        buffer.flip();
        return buffer;
    }

    @Override
    public @NotNull Vector2f read(@NotNull ByteBuffer buffer) {
        return new Vector2f(buffer.getFloat(), buffer.getFloat());
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull Vector2f data) {
        JsonArray array = new JsonArray();
        array.add(data.x);
        array.add(data.y);
        return array;
    }

    @Override
    public @NotNull Vector2f deserialize(@NotNull JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        return new Vector2f(array.get(0).getAsFloat(), array.get(1).getAsFloat());
    }

}
